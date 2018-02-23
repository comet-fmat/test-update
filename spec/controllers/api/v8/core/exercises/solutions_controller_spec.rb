require 'spec_helper'
require 'fileutils'

describe Api::V8::Core::Exercises::SolutionsController, type: :controller do
  let(:organization) { FactoryGirl.create(:accepted_organization) }
  let(:course_name) { 'testcourse' }
  repo_path = Dir.tmpdir + '/api/v8/core/exercises/solutions/remote_repo'
  let(:course) { FactoryGirl.create(:course, name: "#{organization.slug}-#{course_name}", organization: organization, source_backend: 'git', source_url: repo_path) }
  let(:exercise) { FactoryGirl.create(:exercise, name: 'testexercise', course: course) }
  let(:user) { FactoryGirl.create(:user) }
  let(:admin) { FactoryGirl.create(:admin) }

  before :each do
    FileUtils.rm_rf(repo_path)
    create_bare_repo(repo_path)
    controller.stub(:doorkeeper_token) { token }
  end

  describe 'Downloading an exercise solution zip' do
    describe 'as an admin' do
      let(:token) { double resource_owner_id: admin.id, acceptable?: true }

      it 'should succeed if the exercise exists' do
        repo = clone_course_repo(course)
        repo.copy_simple_exercise(exercise.name)
        repo.add_commit_push
        course.refresh

        get :download, exercise_id: exercise.id
        expect(response.code).to eq('200')
      end
    end

    describe 'as a student' do
      let(:token) { double resource_owner_id: user.id, acceptable?: true }

      describe 'if the course exists' do
        it 'should succeed if I have already solved it' do
          repo = clone_course_repo(course)
          repo.copy_simple_exercise(exercise.name)
          repo.add_commit_push
          course.refresh

          FactoryGirl.create(:submission, course: course, user: user, exercise: exercise, all_tests_passed: true)

          get :download, exercise_id: exercise.id
          expect(response).to have_http_status :ok
        end
        it 'should fail if I have not already solved it' do
          repo = clone_course_repo(course)
          repo.copy_simple_exercise(exercise.name)
          repo.add_commit_push
          course.refresh

          FactoryGirl.create(:submission, course: course, user: user, exercise: exercise, all_tests_passed: false)

          get :download, exercise_id: exercise.id
          expect(response).to have_http_status :forbidden
        end
      end
      describe 'if the course does not exist' do
        it 'should fail' do
          get :download, exercise_id: 123
          expect(response).to have_http_status :not_found
        end
      end
    end
  end

  after :all do
    FileUtils.rm_rf(repo_path)
  end
end
