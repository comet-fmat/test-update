require 'spec_helper'

describe Api::V8::Core::Courses::ReviewsController, type: :controller do
  let(:course) { FactoryGirl.create(:course) }
  let(:exercise) { FactoryGirl.create(:exercise, course: course) }
  let(:user2) { FactoryGirl.create(:user) }
  let(:submission) { FactoryGirl.create(:submission, exercise: exercise, course: course, user: user, reviewed: true) }
  let(:submission2) { FactoryGirl.create(:submission, exercise: exercise, course: course, user: user2, reviewed: true) }
  let!(:review) { FactoryGirl.create(:review, reviewer: user, submission: submission) }
  let!(:submission2_review) { FactoryGirl.create(:review, review_body: 'submission2_review body', submission: submission2) }

  before(:each) do
    controller.stub(:doorkeeper_token) { token }
  end

  describe "GET reviews for a course for current user's submissions" do
    describe 'as admin' do
      let(:user) { FactoryGirl.create(:admin) }
      let(:token) { double resource_owner_id: user.id, acceptable?: true }

      describe 'when course id given' do
        it "shows only user's submission's review information" do
          get :index, course_id: course.id
          expect(response).to have_http_status(:success)
          json = JSON.parse response.body
          json[0]['submission_id'].should eq(review.submission.id)
          json[0]['exercise_name'].should eq(exercise.name)
          json[0]['id'].should eq(review.id)
          json[0]['reviewer_name'].should eq(user.username)
          json[0]['review_body'].should eq(review.review_body)
          expect(response.body).not_to include submission2_review.review_body
        end
      end
      describe 'when invalid course id given' do
        it 'shows error about finding course' do
          get :index, course_id: -1
          expect(response).to have_http_status(:missing)
          expect(response.body).to include "Couldn't find Course"
          expect(response.body).not_to include review.review_body
          expect(response.body).not_to include submission2_review.review_body
        end
      end
    end

    describe 'as user' do
      let(:user) { FactoryGirl.create(:user) }
      let(:token) { double resource_owner_id: user.id, acceptable?: true }

      describe 'when course id given' do
        it "shows only user's submission's review information" do
          get :index, course_id: course.id
          expect(response).to have_http_status(:success)
          json = JSON.parse response.body
          json[0]['submission_id'].should eq(review.submission.id)
          json[0]['exercise_name'].should eq(exercise.name)
          json[0]['id'].should eq(review.id)
          json[0]['reviewer_name'].should eq(user.username)
          json[0]['review_body'].should eq(review.review_body)
          expect(response.body).not_to include submission2_review.review_body
        end
      end
      describe 'when invalid course id given' do
        it 'shows error about finding course' do
          get :index, course_id: -1
          expect(response).to have_http_status(:missing)
          expect(response.body).to include "Couldn't find Course"
          expect(response.body).not_to include review.review_body
          expect(response.body).not_to include submission2_review.review_body
        end
      end
    end

    describe 'as guest' do
      let(:user) { Guest.new }
      let(:token) { nil }

      describe 'when course id given' do
        it "shows only user's submission's review information" do
          get :index, course_id: course.id
          expect(response).to have_http_status(403)
          expect(response.body).to include 'Authentication required'
          expect(response.body).not_to include review.review_body
          expect(response.body).not_to include submission2_review.review_body
        end
      end
      describe 'when invalid course id given' do
        it 'shows error about finding course' do
          get :index, course_id: -1
          expect(response).to have_http_status(403)
          expect(response.body).to include 'Authentication required'
          expect(response.body).not_to include review.review_body
          expect(response.body).not_to include submission2_review.review_body
        end
      end
    end
  end

  describe 'Update review info' do
    describe 'As an admin' do
      let(:user) { FactoryGirl.create(:admin) }
      let(:token) { double resource_owner_id: user.id, acceptable?: true }
      before :each do
        controller.current_user = user
      end
      describe 'when correct course id and review id are given' do
        describe 'when review is edited' do
          it 'review text should be updated' do
            put :update, course_id: course.id, id: submission.id, review: { review_body: 'Code looks ok' }
            review.reload
            expect(response).to have_http_status :ok
            expect(review.review_body).to include('Code looks ok')
          end
        end

        describe 'when review is marked as read' do
          it "review's marked_as_read status should change accordingly" do
            put :update, course_id: course.id, id: submission.id, mark_as_read: 1
            review.reload
            expect(response).to have_http_status :ok
            expect(review.marked_as_read).to be_truthy
          end
        end

        describe 'when review is marked as unread' do
          it "review's marked_as_read status should change accordingly" do
            put :update, course_id: course.id, id: submission.id, mark_as_unread: 1
            review.reload
            expect(response).to have_http_status :ok
            expect(review.marked_as_read).to be_falsey
          end
        end
      end
      describe 'when review could not be found' do
        it 'error message is shown' do
          put :update, course_id: course.id, id: 741, mark_as_read: 1
          expect(response).to have_http_status(:not_found)
          expect(response.body).to include("Couldn't find Review")
        end
      end
    end

    describe 'As an user' do
      describe 'when trying to edit review' do
        let(:user) { FactoryGirl.create(:user) }
        let(:token) { double resource_owner_id: user.id, acceptable?: true }
        it 'should deny access' do
          FactoryGirl.create(:review, submission: submission)
          exercise.reload
          put :update, course_id: course.id, id: submission.id, review: { review_body: 'Code looks ok' }
          expect(response).to have_http_status(:forbidden)
          expect(response.body).to include('You are not authorized to access this page')
        end
      end
    end
  end
end
