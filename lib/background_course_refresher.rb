class BackgroundCourseRefresher
  # Called periodically by script/background_daemon, does full refresh on courses which
  # still need full initial refresh.
  def do_refresh
    Course.where(initial_refresh_ready: false).where.not(refreshed_at: nil).each do |course|
      Rails.logger.info "Starting background refresh on course id #{course.id}"
      begin
        course.refresh(no_directory_changes: course.course_template.cache_exists?)
        course.initial_refresh_ready = true
        course.save!
        Rails.logger.info "Finished background refresh on course id #{course.id}"
      rescue => e
        Rails.logger.warn "Background refresh on course id #{course.id} failed!. Error #{e}"
      end
    end
  end
end
