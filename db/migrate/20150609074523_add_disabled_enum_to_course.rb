class AddDisabledEnumToCourse < ActiveRecord::Migration
  def change
    add_column :courses, :disabled_status, :integer, default: 0
  end
end
