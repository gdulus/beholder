(ns beholder.bootstrap
  (:require [beholder.repository :as r]
            [beholder.model :as m]))

(defn init-elastic []
  (do
    (r/delete-indexes!)
    (r/create-indexes!)
    (r/save-documentation! (m/->Documentation nil nil nil "EasyBill" "https://www.easybill.de/api" "new" "swagger"))
    (r/save-documentation! (m/->Documentation nil nil nil "Petstore" "https://petstore.swagger.io" "new" "swagger"))
    (r/save-render! (m/->Render "Petstore" "https://petstore.swagger.io" "Swagger UI Explore Swagger Petstore  1.0.6 [ Base URL: petstore.swagger.io/v2 ] https://petstore.swagger.io/v2/swagger.json This is a sample server Petstore server. You can find out more about Swagger at http://swagger.io or on irc.freenode.net, #swagger. For this sample, you can use the api key special-key to test the authorization filters. Terms of service Contact the developer Apache 2.0Find out more about Swagger Schemeshttpshttp Authorize pet Everything about your Pets Find out more: http://swagger.io POST/pet/{petId}/uploadImage uploads an image POST/pet Add a new pet to the store PUT/pet Update an existing pet GET/pet/findByStatus Finds Pets by status GET/pet/findByTags Finds Pets by tags GET/pet/{petId} Find pet by ID POST/pet/{petId} Updates a pet in the store with form data DELETE/pet/{petId} Deletes a pet store Access to Petstore orders POST/store/order Place an order for a pet GET/store/order/{orderId} Find purchase order by ID DELETE/store/order/{orderId} Delete purchase order by ID GET/store/inventory Returns pet inventories by status user Operations about user Find out more about our store: http://swagger.io POST/user/createWithArray Creates list of users with given input array POST/user/createWithList Creates list of users with given input array GET/user/{username} Get user by user name PUT/user/{username} Updated user DELETE/user/{username} Delete user GET/user/login Logs user into the system GET/user/logout Logs out current logged in user session POST/user Create user Models ApiResponse Category Pet Tag Order User"))
    ))


(try
  (init-elastic)
  (catch Exception e
    (println e)
    ))

(r/search-render! "DELETE")