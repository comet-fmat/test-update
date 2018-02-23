require 'fileutils'

class GitRepo
  include SystemCommands

  attr_reader :path

  def initialize(path)
    @path = File.expand_path(path)
    @commit_count = 0
    @active_branch = 'master'
  end

  attr_reader :active_branch

  attr_writer :active_branch

  def copy_simple_exercise(dest_name = nil, metadata = {})
    copy_fixture_exercise('SimpleExercise', dest_name, metadata)
  end

  def copy_fixture_exercise(src_name, dest_name = nil, metadata = {})
    dest_name ||= src_name

    dest = "#{@path}/#{dest_name}"

    ex = FixtureExercise.get(src_name, dest)
    ex.write_metadata(metadata) unless metadata.empty?
    ex
  end

  def copy(src, dest_name = nil)
    dest_name ||= Pathname(src).basename
    dest = "#{@path}/#{dest_name}"
    FileUtils.cp_r(src, dest)
  end

  def set_metadata_in(dir, metadata_hash)
    write_file("#{dir}/metadata.yml", metadata_hash.to_yaml)
  end

  def mkdir(name)
    fail 'Expected relative path' if name.start_with?('/')
    FileUtils.mkdir_p("#{@path}/#{name}")
  end

  def write_file(name, content)
    fail 'Expected relative path' if name.start_with?('/')
    File.open("#{@path}/#{name}", 'wb') { |f| f.write(content) }
  end

  def debug_list_files
    system('find', @path)
  end

  def add_commit_push
    add
    commit
    push
  end

  def add
    chdir do
      system!('git add -A')
    end
  end

  def commit
    chdir do
      @commit_count += 1
      system!("git commit -q -m 'commit #{@commit_count} from test case'")
    end
  end

  def push
    chdir do
      system!("git push -q origin #{active_branch} >/dev/null 2>&1")
    end
  end

  def chdir(&block)
    Dir.chdir @path do
      block.call
    end
  end
end
