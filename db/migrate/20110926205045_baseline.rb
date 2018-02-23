class Baseline < ActiveRecord::Migration
  def self.up
    create_table "available_points", force: true do |t|
      t.integer "exercise_id", null: false
      t.string  "name",        null: false
    end

    create_table "awarded_points", force: true do |t|
      t.integer "course_id",     null: false
      t.integer "user_id",       null: false
      t.integer "submission_id"
      t.string  "name",          null: false
    end

    create_table "courses", force: true do |t|
      t.string   "name"
      t.datetime "created_at"
      t.datetime "updated_at"
      t.datetime "hide_after"
      t.string   "remote_repo_url"
      t.boolean  "hidden",          default: false, null: false
      t.integer  "cache_version",   default: 0,     null: false
      t.string   "spreadsheet_key"
    end

    create_table "exercises", force: true do |t|
      t.string   "name"
      t.datetime "created_at"
      t.datetime "updated_at"
      t.integer  "course_id"
      t.datetime "deadline"
      t.datetime "publish_date"
      t.string   "gdocs_sheet"
      t.boolean  "hidden",            default: false, null: false
      t.boolean  "returnable_forced"
    end

    add_index "exercises", ["name"], name: "index_exercises_on_name"

    create_table "points_upload_queues", force: true do |t|
      t.integer  "point_id"
      t.datetime "created_at"
      t.datetime "updated_at"
    end

    create_table "sessions", force: true do |t|
      t.string   "session_id", null: false
      t.text     "data"
      t.datetime "created_at"
      t.datetime "updated_at"
    end

    add_index "sessions", ["session_id"], name: "index_sessions_on_session_id"
    add_index "sessions", ["updated_at"], name: "index_sessions_on_updated_at"

    create_table "submissions", force: true do |t|
      t.integer  "user_id"
      t.binary   "return_file"
      t.text     "pretest_error"
      t.datetime "created_at"
      t.datetime "updated_at"
      t.string   "exercise_name", null: false
      t.integer  "course_id",     null: false
    end

    add_index "submissions", ["course_id", "exercise_name"], name: "index_submissions_on_course_id_and_exercise_name"

    create_table "test_case_runs", force: true do |t|
      t.integer  "submission_id"
      t.text     "test_case_name"
      t.string   "message"
      t.boolean  "successful"
      t.datetime "created_at"
      t.datetime "updated_at"
    end

    create_table "users", force: true do |t|
      t.string   "login",                                           null: false
      t.text     "password_hash"
      t.datetime "created_at"
      t.datetime "updated_at"
      t.string   "salt"
      t.boolean  "administrator",                default: false, null: false
    end
  end

  def self.down
    raise 'irreversible'
  end
end
