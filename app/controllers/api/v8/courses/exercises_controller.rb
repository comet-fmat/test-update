module Api
  module V8
    module Courses
      class ExercisesController < Api::V8::BaseController
        include Swagger::Blocks

        swagger_path '/api/v8/courses/{course_id}/exercises' do
          operation :get do
            key :description, 'Returns all exercises of the course as json. Course is searched by id'
            key :operationId, 'findExercisesById'
            key :produces, ['application/json']
            key :tags, ['exercise']
            parameter '$ref': '#/parameters/path_course_id'
            response 200 do
              key :description, 'Exercises in json'
              schema do
                key :title, :exercises
                key :required, [:exercises]
                property :exercises do
                  key :type, :array
                  items do
                    key :'$ref', :ExerciseWithPoints
                  end
                end
              end
            end
            response 403, '$ref': '#/responses/error'
            response 404, '$ref': '#/responses/error'
          end
        end

        def index
          unauthorize_guest!
          course = Course.find_by!(id: params[:course_id]) if params[:course_id]
          authorize! :read, course
          exercises = Exercise.includes(:available_points).where(course_id: course.id)

          unlocked_exercises = course.unlocks
                                     .where(user_id: current_user.id)
                                     .where(['valid_after IS NULL OR valid_after < ?', Time.now])
                                     .pluck(:exercise_name)

          unless current_user.administrator? || current_user.teacher?(course.organization) || current_user.assistant?(course)
            exercises = exercises.where(hidden: false, disabled_status: 0)
            exercises = if unlocked_exercises.empty?
                          exercises.where(unlock_spec: nil)
                        else
                          exercises.where(["unlock_spec IS NULL OR name IN (#{unlocked_exercises.map { |_| '?' }.join(', ')})", *unlocked_exercises])
                        end.select { |e| e._fast_visible_to?(current_user) }
          end

          presentable = exercises.map do |ex|
            {
              id: ex.id,
              available_points: ex.available_points,
              name: ex.name,
              publish_time: ex.publish_time,
              solution_visible_after: ex.solution_visible_after,
              deadline: ex.deadline_for(current_user),
              disabled: ex.disabled?
            }
          end

          render json: presentable
        end
      end
    end
  end
end
