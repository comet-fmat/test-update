class RenameUserToSender < ActiveRecord::Migration
  def up
    rename_column :course_notifications, :user_id, :sender_id
  end

  def down
    rename_column :course_notifications, :sender_id, :user_id
  end
end
