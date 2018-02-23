require 'paths'
require 'subprocess_group_with_timeout'
require 'shell_utils'
require 'shellwords'
require 'app_log'
require 'uml_instance'
require 'tap_device'

class SandboxInstance
  attr_reader :index  # 0-based

  def initialize(index, settings, plugin_manager)
    @index = index
    @settings = settings
    @plugin_manager = plugin_manager
    nuke_work_dir!

    @instance = UmlInstance.new

    @instance.subprocess_init do
      wait_for_cooldown

      ShellUtils.sh!(["dd", "if=/dev/zero", "of=#{output_tar_path}", "bs=#{@settings['max_output_size']}", "count=1"])
    end

    @instance.when_done do |process_status|
      write_exit_time

      exit_code = nil
      status =
        if process_status == :timeout
          :timeout
        elsif process_status.success?
          begin
            exit_code = extract_file_from_tar(output_tar_path, 'exit_code.txt').to_i
          rescue
            warn "Failed to untar exit_code.txt"
            exit_code = nil
          end
          if exit_code == 0
            :finished
          else
            :failed
          end
        else
          warn "Sandbox failed with status #{process_status.inspect}"
          :failed
        end

      debug "Status: #{status}. Exit code: #{exit_code.inspect}."

      output = {
        'test_output' => try_extract_file_from_tar(output_tar_path, 'test_output.txt'),
        'stdout' => try_extract_file_from_tar(output_tar_path, 'stdout.txt'),
        'stderr' => try_extract_file_from_tar(output_tar_path, 'stderr.txt'),
        'valgrind' => try_extract_file_from_tar(output_tar_path, 'valgrind.log'),
        'validations' => try_extract_file_from_tar(output_tar_path, 'validations.json'),
        'vm_log' => File.read(vm_log_path)
      }

      @notifier.call(status, exit_code, output) if @notifier
    end
  end

  # Runs the sandbox. Notifier is called with
  # status string, exit code, output hash
  def start(tar_file, &notifier)
    raise 'busy' if busy?

    read_exit_time
    nuke_work_dir!
    @notifier = notifier
    FileUtils.mv(tar_file, task_tar_path)

    @plugin_images = @plugin_manager.run_hook(:extra_images, :instance => self).reduce({}, &:merge)

    @plugin_manager.run_hook(:before_exec, :instance => self, :tar_file => task_tar_path)

    file_locks = @plugin_images.map {|name, path|
      if name.to_s =~ /^(ubd.)c?(r?)c?$/
        lock_type = if $2 == 'r' then File::LOCK_SH else File::LOCK_EX end
        [path, lock_type]
      else
        raise "Invalid plugin image name: #{name}"
      end
    }

    @instance.set_options({
      :disks => @plugin_images.merge({
        :ubdarc => Paths.rootfs_path,
        :ubdbr => task_tar_path,
        :ubdc => output_tar_path
      }),
      :file_locks => file_locks,
      :mem => @settings['instance_ram'],
      :network => network_devices,
      :timeout => @settings['timeout'].to_i,
      :vm_log => vm_log_path
    })

    @instance.start
  end

  def idle?
    !busy?
  end

  def busy?
    @instance.running?
  end

  def wait
    @instance.wait
  end

  def kill
    @instance.kill
  end

private

  def nuke_work_dir!
    debug "Clearing work dir"
    FileUtils.rm_rf instance_work_dir
    FileUtils.mkdir_p instance_work_dir
  end

  def instance_work_dir
    Paths.work_dir + @index.to_s
  end

  def output_tar_path
    instance_work_dir + 'output.tar'
  end

  def task_tar_path
    instance_work_dir + 'task.tar'
  end

  def vm_log_path
    instance_work_dir + 'vm.log'
  end

  def extract_file_from_tar(tar_path, file_name)
    result = `tar --to-stdout -xf #{output_tar_path} #{file_name} 2>/dev/null`
    raise "Failed to extract #{file_name} from #{tar_path}" if !$?.success?
    result
  end

  def try_extract_file_from_tar(tar_path, file_name)
    begin
      extract_file_from_tar(tar_path, file_name)
    rescue
      ""
    end
  end

  def network_devices
    if @settings['network'] && @settings['network']['enabled']
      i = @index
      tapdev = "tap_tmc#{i}"
      ip_range_start = @settings['network']['private_ip_range_start']
      ip = "192.168.#{ip_range_start + i}.1"
      {:eth0 => TapDevice.new(tapdev, ip, Settings.tmc_user)}
    else
      {}
    end
  end

  def debug(msg)
    log_with_level(:debug, msg)
  end

  def warn(msg)
    log_with_level(:warn, msg)
  end

  def error(msg)
    log_with_level(:error, msg)
  end

  def log_with_level(level, msg)
    AppLog.send(level, "Instance #{@index}: #{msg}")
  end


  # We have problems with network setup giving the following errors
  # TUNSETIFF failed, errno = 16
  # SIOCSIFFLAGS: Device or resource busy
  # This is hard to debug because the error is pretty rare.
  # We hope that adding a cooldown between the uses of the same
  # instance (the same tap device) will avoid this.
  def write_exit_time
    begin
      File.open(exit_time_file, 'wb') do |f|
        f.write(Time.now.to_f)
      end
    rescue
      warn("Failed to write exit_time.tmp: #{$!}")
    end
  end

  def read_exit_time
    @exit_time = nil
    if File.exist?(exit_time_file)
      begin
        @exit_time = Time.at(File.read(exit_time_file).to_f)
      rescue
        warn("Failed to read exit_time.tmp")
      end
    end
  end

  def wait_for_cooldown
    if @exit_time
      time_to_wait = (@exit_time + 3) - Time.now
      if time_to_wait > 0
        debug("Waiting #{time_to_wait} for cooldown of instance #{index}")
        sleep(time_to_wait)
      end
    end
  end

  def exit_time_file
    instance_work_dir + 'exit_time.tmp'
  end
end
