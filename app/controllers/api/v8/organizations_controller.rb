module Api
  module V8
    class OrganizationsController < Api::V8::BaseController
      include Swagger::Blocks

      swagger_path '/api/v8/org.json' do
        operation :get do
          key :description, 'Returns a list of all organizations'
          key :operationId, 'findOrganizations'
          key :produces, ['application/json']
          key :tags, ['organization']
          response 403, '$ref': '#/responses/error'
          response 404, '$ref': '#/responses/error'
          response 200 do
            key :description, 'List of organizations in json'
            schema do
              key :type, :array
              items do
                key :'$ref', :Organization
              end
            end
          end
        end
      end

      swagger_path '/api/v8/org/{organization_slug}.json' do
        operation :get do
          key :description, 'Returns a json representation of the organization'
          key :operationId, 'findOrganization'
          key :produces, ['application/json']
          key :tags, ['organization']
          parameter '$ref': '#/parameters/path_organization_slug'
          response 403, '$ref': '#/responses/error'
          response 404, '$ref': '#/responses/error'
          response 200 do
            key :description, 'Organization in json'
            schema do
              key :title, :organization
              key :required, [:organization]
              property :organization, '$ref': :Organization
            end
          end
        end
      end

      def index
        orgs = Organization.visible_organizations.map { |o| { name: o.name, information: o.information, slug: o.slug, logo_path: o.logo.url, pinned: o.pinned } }
        authorize! :read, orgs
        present(orgs)
      end

      def show
        org = Organization.find_by!(slug: params[:slug])
        authorize! :read, org
        present org.org_as_json
      end
    end
  end
end
