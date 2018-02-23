class AddPasswordResetKey < ActiveRecord::Migration
  def change
    create_table :password_reset_keys do |t|
      t.integer :user_id, null: false
      t.text :code, null: false
      t.datetime :created_at, null: false
    end
  end
end
