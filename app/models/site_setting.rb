# Represents the contents of `config/site.yml`.
class SiteSetting
  def self.value(key)
    key = key.to_s
    fail "No such setting: #{key}" unless all_settings.key?(key)
    all_settings[key]
  end

  def self.all_settings
    @settings ||= settings_from_files(settings_files)
  end

  def self.use_distribution_defaults! # for tests
    @settings = settings_from_files(settings_files.select { |f| f.end_with?('site.defaults.yml') })
  end

  private

  def self.settings_from_files(files)
    result = {}
    files.each do |path|
      if File.exist?(path)
        template = ERB.new File.new(path).read
        data = YAML.load template.result(binding)
        fail "Invalid configuration file #{path}" unless data.is_a? Hash
        result = result.deep_merge(data)
      end
    end
    result
  end

  def self.settings_files
    config_dir = "#{Rails.root}/config"
    ["#{config_dir}/site.defaults.yml", "#{config_dir}/site.yml"]
  end
end
