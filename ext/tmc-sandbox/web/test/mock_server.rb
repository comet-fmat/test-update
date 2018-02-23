require 'net/http'
require 'webrick'

class MockServer
  def url
    "http://localhost:11988/notify"
  end

  def interact(&block)
    @req_data = nil
    
    pipe_block do |pipe_in, pipe_out|
      server_pid = fork_server_process(pipe_in, pipe_out)
      pipe_out.close
      
      begin
        reader = Thread.fork do
          begin
            @req_data = MultiJson.decode(pipe_in.read)
          rescue
            @req_data = nil
          end
        end
      
        wait_for_server_to_be_ready
        block.call
      ensure
        if reader
          reader.join(2)
          pipe_in.close
          reader.join rescue StandardError
        end
        
        Process.kill("KILL", -server_pid) # Kill by process group.
        Process.waitpid(server_pid)
      end
    end
    
    @req_data
  end
  
private
  def fork_server_process(pipe_in, pipe_out)
    Process.fork do
      Process.setpgrp # Give the server its own process group so its process tree can be killed more reliably
      $stdin.close
      $stdout.close
      $stderr.close
      pipe_in.close
      
      app = Rack::Builder.new do
        map '/ready' do
          ready = lambda do
            [200, {'Content-Type' => 'text/plain'}, ["ready"]]
          end
          run ready
        end
        
        map '/notify' do
          notify = lambda do |env|
            req = Rack::Request.new(env)
            pipe_out.write(MultiJson.encode({:content_type => env['CONTENT_TYPE'], :params => req.params}))
            pipe_out.close
            [200, {'Content-Type' => 'text/plain'}, ["OK"]]
          end
          run notify
        end
      end.to_app
      
      webrick_log = WEBrick::Log.new('/dev/null')
      webrick_opts = {
        :Host => 'localhost',
        :Port => 11988,
        :Logger => webrick_log,
        :AccessLog => [ [webrick_log, WEBrick::AccessLog::COMBINED_LOG_FORMAT] ]
      }
      Rack::Handler::WEBrick.run(app, webrick_opts)
    end
  end
  
  def wait_for_server_to_be_ready
    deadline = Time.now + 10
    while Time.now < deadline
      begin
        Net::HTTP.get(URI('http://localhost:11988/ready'))
        return
      rescue Errno::ECONNREFUSED
        sleep 0.1
      end
    end
    raise 'MockServer timed out waiting for server to become ready'
  end
  
  def pipe_block(&block)
    # IO.pipe with a block not supported on Ruby 1.8.7
    infd, outfd = IO.pipe
    begin
      block.call(infd, outfd)
    ensure
      infd.close unless infd.closed?
      outfd.close unless outfd.closed?
    end
  end
end
