if LogStasher.enabled?
  LogStasher.add_custom_fields do |fields|
    # This block is run in application_controller context,
    # so you have access to all controller methods
    begin
      if current_user && current_user.login
        fields[:user] = current_user.login
      else
        fields[:user] = 'guest'
      end
    rescue NameError
      fields[:user] = 'pghero/api?'
    end

    begin
      if params
        fields[:client] = params[:client] if params[:client]
        fields[:client_version] = params[:client_version] if params[:client_version]
      end
    rescue NameError
    end

    fields[:site] = request.path =~ /^\/api/ ? 'api' : 'user'

    # If you are using custom instrumentation, just add it to logstasher custom fields
    LogStasher.custom_fields << :myapi_runtime
  end
end
