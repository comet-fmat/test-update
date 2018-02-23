class AddIndexesToHelpPointsViews < ActiveRecord::Migration
  def change
    add_index :exercises, [:gdocs_sheet]
    add_index :awarded_points, [:course_id, :user_id, :submission_id]
  end
end
