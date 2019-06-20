(ns com.wsscode.pathom.connect.graphql2-test
  (:require [clojure.test :refer [is are testing]]
            [clojure.walk :as walk]
            [camel-snake-kebab.core :as csk]
            [nubank.workspaces.core :refer [deftest]]
            [com.wsscode.pathom.core :as p]
            [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom.connect.graphql2 :as pcg]
            [com.wsscode.pathom.graphql :as pg]))

(def query-root-type
  (pcg/normalize-schema
    {:name   "QueryRoot"
     :fields [{:name "banks" :args [] :type {:kind "LIST" :name nil :ofType {:kind "OBJECT" :name "Bank"}}}
              {:name "creditCardAccount"
               :args [{:name "customerId" :defaultValue nil :type {:kind "SCALAR" :name "ID"}}]
               :type {:kind "OBJECT" :name "CreditCardAccount" :ofType nil}}
              {:name "customer"
               :args [{:name "customerId" :defaultValue nil :type {:kind "SCALAR" :name "ID"}}]
               :type {:kind :OBJECT :name "Customer" :ofType nil}}
              {:name "repository"
               :args [{:name "owner" :defaultValue nil :type {:kind "SCALAR" :name "String"}}
                      {:name "name" :defaultValue nil :type {:kind "SCALAR" :name "String"}}]
               :type {:kind "OBJECT" :name "Repository" :ofType nil}}
              {:name "savingsAccount"
               :args [{:name "customerId" :defaultValue nil :type {:kind "SCALAR" :name "ID"}}]
               :type {:kind "OBJECT" :name "SavingsAccount" :ofType nil}}
              {:name "viewer" :args [] :type {:kind "OBJECT" :name "Customer" :ofType nil}}]}))

(def customer-type
  {:name       "Customer"
   :kind       "OBJECT"
   :interfaces []
   :fields     [{:name "id" :args [] :type {:kind "NON_NULL" :name nil :ofType {:kind "SCALAR" :name "ID"}}}
                {:name "cpf" :args [] :type {:kind "NON_NULL" :name nil :ofType {:kind "SCALAR" :name "String"}}}
                {:name "creditCardAccount" :args [] :type {:kind "OBJECT" :name "CreditCardAccount" :ofType nil}}
                {:name "feed" :args [] :type {:kind "LIST" :name nil :ofType {:kind "NON_NULL" :name nil :ofType {:kind "INTERFACE" :name "FeedEvent"}}}}
                {:name "name" :args [] :type {:kind "NON_NULL" :name nil :ofType {:kind "SCALAR" :name "String"}}}
                {:name "preferredName" :args [] :type {:kind "SCALAR" :name "String" :ofType nil}}
                {:name "savingsAccount" :args [] :type {:kind "OBJECT" :name "SavingsAccount" :ofType nil}}]})

(def repository-type
  {:name       "Repository"
   :kind       "OBJECT"
   :interfaces []
   :fields     [{:name "id" :args [] :type {:kind "NON_NULL" :name nil :ofType {:kind "SCALAR" :name "ID"}}}
                {:name "name" :args [] :type {:kind "NON_NULL" :name nil :ofType {:kind "SCALAR" :name "String"}}}]})

(def feed-event-interface
  {:name       "FeedEvent"
   :kind       "INTERFACE"
   :interfaces []
   :fields     [{:name "detail" :args [] :type {:kind "NON_NULL" :name nil :ofType {:kind "SCALAR" :name "String"}}}
                {:name "id" :args [] :type {:kind "NON_NULL" :name nil :ofType {:kind "SCALAR" :name "ID"}}}
                {:name "postDate" :args [] :type {:kind "NON_NULL" :name nil :ofType {:kind "SCALAR" :name "Date"}}}
                {:name "title" :args [] :type {:kind "NON_NULL" :name nil :ofType {:kind "SCALAR" :name "String"}}}]})

(def onboarding-event-type
  {:name       "OnboardingEvent"
   :kind       "OBJECT"
   :interfaces [{:name "FeedEvent" :kind "INTERFACE"}]
   :fields     [{:name "detail" :args [] :type {:kind "NON_NULL" :name nil :ofType {:kind "SCALAR" :name "String"}}}
                {:name "id" :args [] :type {:kind "NON_NULL" :name nil :ofType {:kind "SCALAR" :name "ID"}}}
                {:name "postDate" :args [] :type {:kind "NON_NULL" :name nil :ofType {:kind "SCALAR" :name "Date"}}}
                {:name "title" :args [] :type {:kind "NON_NULL" :name nil :ofType {:kind "SCALAR" :name "String"}}}]})

(def mutation-type
  {:name   "Mutation"
   :fields [{:name "addStar"}
            {:name "removeStar"}
            {:name "requestReviews"}]})

(def types
  [{:name       "CreditCardBalances"
    :kind       "OBJECT"
    :interfaces []
    :fields     [{:name "available" :args [] :type {:kind "SCALAR" :name "Float" :ofType nil}}
                 {:name "due" :args [] :type {:kind "NON_NULL" :name nil :ofType {:kind "SCALAR" :name "Float"}}}
                 {:name "future" :args [] :type {:kind "NON_NULL" :name nil :ofType {:kind "SCALAR" :name "Float"}}}
                 {:name "open" :args [] :type {:kind "NON_NULL" :name nil :ofType {:kind "SCALAR" :name "Float"}}}
                 {:name "prepaid" :args [] :type {:kind "SCALAR" :name "Float" :ofType nil}}]}
   query-root-type
   customer-type
   repository-type
   feed-event-interface
   onboarding-event-type
   mutation-type])

(def schema
  {:__schema
   {:queryType    {:name "QueryRoot"}
    :mutationType {:name "Mutation"}
    :types        types}})

(def prefix "service")
(def env {::pcg/prefix "service" ::pcg/mung identity})

(deftest test-type-key
  (is (= (pcg/type-key env "CreditCardBalances")
         :service.types/CreditCardBalances)))

(deftest test-interface-key
  (is (= (pcg/interface-key env "FeedEvent")
         :service.interfaces/FeedEvent)))

(deftest test-type->field-entry
  (is (= (pcg/type->field-entry env {:kind "SCALAR" :name "Float" :ofType nil})
         {}))
  (is (= (pcg/type->field-entry env {:kind "OBJECT" :name "CreditCardAccount" :ofType nil})
         {:service.types/CreditCardAccount {}}))
  (is (= (pcg/type->field-entry env {:kind "INTERFACE" :name "FeedEvent" :ofType nil})
         {:service.interfaces/FeedEvent {}}))
  (is (= (pcg/type->field-entry env {:kind "NON_NULL" :name nil :ofType {:kind "SCALAR" :name "String"}})
         {}))
  (is (= (pcg/type->field-entry env {:kind "NON_NULL" :name nil :ofType {:kind "OBJECT" :name "CreditCardAccount" :ofType nil}})
         {:service.types/CreditCardAccount {}}))
  (is (= (pcg/type->field-entry env {:kind "LIST" :name nil :ofType {:kind "OBJECT" :name "Bank"}})
         {:service.types/Bank {}})))

(deftest test-index-type
  (is (= (pcg/index-type env customer-type)
         {#{:service.types/Customer} #:service.Customer{:cpf               {}
                                                        :creditCardAccount #:service.types{:CreditCardAccount {}}
                                                        :feed              #:service.interfaces{:FeedEvent {}}
                                                        :id                {}
                                                        :name              {}
                                                        :preferredName     {}
                                                        :savingsAccount    #:service.types{:SavingsAccount {}}}}))

  (is (= (pcg/index-type env feed-event-interface)
         {#{:service.interfaces/FeedEvent} #:service.FeedEvent{:detail   {}
                                                               :id       {}
                                                               :postDate {}
                                                               :title    {}}}))

  (is (= (pcg/index-type env onboarding-event-type)
         {#{:service.types/OnboardingEvent} {:service.OnboardingEvent/detail   {}
                                             :service.OnboardingEvent/id       {}
                                             :service.OnboardingEvent/postDate {}
                                             :service.OnboardingEvent/title    {}
                                             :service.interfaces/FeedEvent     {}}})))

(def supposed-resolver nil)

(def indexes
  `{:com.wsscode.pathom.connect.graphql2/field->ident {:service.Customer/cpf               #:com.wsscode.pathom.connect.graphql2{:entity-field :service.Customer/id
                                                                                                                                 :ident-key    :customer/customerId}
                                                       :service.Customer/creditCardAccount #:com.wsscode.pathom.connect.graphql2{:entity-field :service.Customer/id
                                                                                                                                 :ident-key    :customer/customerId}
                                                       :service.Customer/feed              #:com.wsscode.pathom.connect.graphql2{:entity-field :service.Customer/id
                                                                                                                                 :ident-key    :customer/customerId}
                                                       :service.Customer/id                #:com.wsscode.pathom.connect.graphql2{:entity-field :service.Customer/id
                                                                                                                                 :ident-key    :customer/customerId}
                                                       :service.Customer/name              #:com.wsscode.pathom.connect.graphql2{:entity-field :service.Customer/id
                                                                                                                                 :ident-key    :customer/customerId}
                                                       :service.Customer/preferredName     #:com.wsscode.pathom.connect.graphql2{:entity-field :service.Customer/id
                                                                                                                                 :ident-key    :customer/customerId}
                                                       :service.Customer/savingsAccount    #:com.wsscode.pathom.connect.graphql2{:entity-field :service.Customer/id
                                                                                                                                 :ident-key    :customer/customerId}
                                                       :service.Repository/id              #:com.wsscode.pathom.connect.graphql2{:entity-field [:service.Customer/name
                                                                                                                                                :service.Repository/name]
                                                                                                                                 :ident-key    :repository/owner-and-name}
                                                       :service.Repository/name            #:com.wsscode.pathom.connect.graphql2{:entity-field [:service.Customer/name
                                                                                                                                                :service.Repository/name]
                                                                                                                                 :ident-key    :repository/owner-and-name}}
    :com.wsscode.pathom.connect/autocomplete-ignore   #{:service.interfaces/FeedEvent
                                                        :service.types/CreditCardBalances
                                                        :service.types/Customer
                                                        :service.types/OnboardingEvent
                                                        :service.types/Repository}
    :com.wsscode.pathom.connect/idents                #{:service.Customer/id}
    :com.wsscode.pathom.connect/index-io              {#{:service.Customer/id}              #:service.types{:CreditCardAccount {}
                                                                                                            :Customer          {}
                                                                                                            :SavingsAccount    {}}
                                                       #{:service.Customer/name
                                                         :service.Repository/name}          #:service.types{:Repository {}}
                                                       #{:service.interfaces/FeedEvent}     #:service.FeedEvent{:detail   {}
                                                                                                                :id       {}
                                                                                                                :postDate {}
                                                                                                                :title    {}}
                                                       #{:service.types/CreditCardBalances} #:service.CreditCardBalances{:available {}
                                                                                                                         :due       {}
                                                                                                                         :future    {}
                                                                                                                         :open      {}
                                                                                                                         :prepaid   {}}
                                                       #{:service.types/Customer}           #:service.Customer{:cpf               {}
                                                                                                               :creditCardAccount #:service.types{:CreditCardAccount {}}
                                                                                                               :feed              #:service.interfaces{:FeedEvent {}}
                                                                                                               :id                {}
                                                                                                               :name              {}
                                                                                                               :preferredName     {}
                                                                                                               :savingsAccount    #:service.types{:SavingsAccount {}}}
                                                       #{:service.types/OnboardingEvent}    {:service.OnboardingEvent/detail   {}
                                                                                             :service.OnboardingEvent/id       {}
                                                                                             :service.OnboardingEvent/postDate {}
                                                                                             :service.OnboardingEvent/title    {}
                                                                                             :service.interfaces/FeedEvent     {}}
                                                       #{:service.types/Repository}         #:service.Repository{:id   {}
                                                                                                                 :name {}}
                                                       #{}                                  #:service{:banks             #:service.types{:Bank {}}
                                                                                                      :creditCardAccount #:service.types{:CreditCardAccount {}}
                                                                                                      :customer          #:service.types{:Customer {}}
                                                                                                      :repository        #:service.types{:Repository {}}
                                                                                                      :savingsAccount    #:service.types{:SavingsAccount {}}
                                                                                                      :viewer            #:service.types{:Customer {}}}}
    :com.wsscode.pathom.connect/index-mutations       {com.wsscode.pathom.connect.graphql.service-mutations/service #:com.wsscode.pathom.connect{:sym com.wsscode.pathom.connect.graphql.service-mutations/service}
                                                       service/addStar                                              #:com.wsscode.pathom.connect{:sym com.wsscode.pathom.connect.graphql.service-mutations/service}
                                                       service/removeStar                                           #:com.wsscode.pathom.connect{:sym com.wsscode.pathom.connect.graphql.service-mutations/service}
                                                       service/requestReviews                                       #:com.wsscode.pathom.connect{:sym com.wsscode.pathom.connect.graphql.service-mutations/service}}
    :com.wsscode.pathom.connect/index-oir             {:service.Customer/cpf               {#{:service.Customer/id} #{com.wsscode.pathom.connect.graphql2-test/supposed-resolver}}
                                                       :service.Customer/creditCardAccount {#{:service.Customer/id} #{com.wsscode.pathom.connect.graphql2-test/supposed-resolver}}
                                                       :service.Customer/feed              {#{:service.Customer/id} #{com.wsscode.pathom.connect.graphql2-test/supposed-resolver}}
                                                       :service.Customer/id                {#{:service.Customer/id} #{com.wsscode.pathom.connect.graphql2-test/supposed-resolver}}
                                                       :service.Customer/name              {#{:service.Customer/id} #{com.wsscode.pathom.connect.graphql2-test/supposed-resolver}}
                                                       :service.Customer/preferredName     {#{:service.Customer/id} #{com.wsscode.pathom.connect.graphql2-test/supposed-resolver}}
                                                       :service.Customer/savingsAccount    {#{:service.Customer/id} #{com.wsscode.pathom.connect.graphql2-test/supposed-resolver}}
                                                       :service.Repository/id              {#{:service.Customer/name
                                                                                              :service.Repository/name} #{com.wsscode.pathom.connect.graphql2-test/supposed-resolver}}
                                                       :service.Repository/name            {#{:service.Customer/name
                                                                                              :service.Repository/name} #{com.wsscode.pathom.connect.graphql2-test/supposed-resolver}}
                                                       :service/banks                      {#{} #{com.wsscode.pathom.connect.graphql2-test/supposed-resolver}}
                                                       :service/creditCardAccount          {#{} #{com.wsscode.pathom.connect.graphql2-test/supposed-resolver}}
                                                       :service/customer                   {#{} #{com.wsscode.pathom.connect.graphql2-test/supposed-resolver}}
                                                       :service/repository                 {#{} #{com.wsscode.pathom.connect.graphql2-test/supposed-resolver}}
                                                       :service/savingsAccount             {#{} #{com.wsscode.pathom.connect.graphql2-test/supposed-resolver}}
                                                       :service/viewer                     {#{} #{com.wsscode.pathom.connect.graphql2-test/supposed-resolver}}}
    :com.wsscode.pathom.connect/index-resolvers       #:com.wsscode.pathom.connect.graphql2-test{supposed-resolver {:com.wsscode.pathom.connect.graphql2/graphql? true
                                                                                                                    :com.wsscode.pathom.connect/cache?            false
                                                                                                                    :com.wsscode.pathom.connect/sym               com.wsscode.pathom.connect.graphql2-test/supposed-resolver}}})

(deftest test-index-schema
  (is (= (-> (pcg/index-schema {::pcg/prefix    prefix ::pcg/schema schema
                                ::pcg/ident-map {"customer"          {"customerId" :service.Customer/id}
                                                 "creditCardAccount" {"customerId" :service.Customer/id}
                                                 "savingsAccount"    {"customerId" :service.Customer/id}
                                                 "repository"        {"owner" :service.Customer/name
                                                                      "name"  :service.Repository/name}}
                                ::pcg/resolver  `supposed-resolver})
             (update-in [::pc/index-resolvers `supposed-resolver] dissoc ::pc/compute-output ::pc/resolve)
             (update-in [::pc/index-mutations 'com.wsscode.pathom.connect.graphql.service-mutations/service] dissoc ::pc/mutate))
         indexes)))

(def builtin-type-names #{"ID" "String" "Date" "Float"})

(def snake-schema (walk/postwalk (fn [{:keys [name] :as v}]
                                   (cond-> v
                                     (and (map? v)
                                          name
                                          (not (builtin-type-names name)))
                                     (update :name csk/->snake_case)))
                                 schema))

(def munged-indexes
  `{:com.wsscode.pathom.connect.graphql2/field->ident {:service.customer/cpf                 #:com.wsscode.pathom.connect.graphql2 {:entity-field :service.customer/id
                                                                                                                                    :ident-key    :customer/customer-id}
                                                       :service.customer/credit-card-account #:com.wsscode.pathom.connect.graphql2 {:entity-field :service.customer/id
                                                                                                                                    :ident-key    :customer/customer-id}
                                                       :service.customer/feed                #:com.wsscode.pathom.connect.graphql2 {:entity-field :service.customer/id
                                                                                                                                    :ident-key    :customer/customer-id}
                                                       :service.customer/id                  #:com.wsscode.pathom.connect.graphql2 {:entity-field :service.customer/id
                                                                                                                                    :ident-key    :customer/customer-id}
                                                       :service.customer/name                #:com.wsscode.pathom.connect.graphql2 {:entity-field :service.customer/id
                                                                                                                                    :ident-key    :customer/customer-id}
                                                       :service.customer/preferred-name      #:com.wsscode.pathom.connect.graphql2 {:entity-field :service.customer/id
                                                                                                                                    :ident-key    :customer/customer-id}
                                                       :service.customer/savings-account     #:com.wsscode.pathom.connect.graphql2 {:entity-field :service.customer/id
                                                                                                                                    :ident-key    :customer/customer-id}
                                                       :service.repository/id                #:com.wsscode.pathom.connect.graphql2 {:entity-field [:service.customer/name
                                                                                                                                                   :service.repository/name]
                                                                                                                                    :ident-key    :repository/owner-and-name}
                                                       :service.repository/name              #:com.wsscode.pathom.connect.graphql2 {:entity-field [:service.customer/name
                                                                                                                                                   :service.repository/name]
                                                                                                                                    :ident-key    :repository/owner-and-name}}
    :com.wsscode.pathom.connect/autocomplete-ignore   #{:service.interfaces/feed-event
                                                        :service.types/credit-card-balances
                                                        :service.types/customer
                                                        :service.types/onboarding-event
                                                        :service.types/repository}
    :com.wsscode.pathom.connect/idents                #{:service.customer/id}
    :com.wsscode.pathom.connect/index-io              {#{:service.customer/id}                #:service.types                {:credit-card-account {}
                                                                                                                              :customer            {}
                                                                                                                              :savings-account     {}}
                                                       #{:service.customer/name
                                                         :service.repository/name}            #:service.types                {:repository {}}
                                                       #{:service.interfaces/feed-event}      #:service.feed-event           {:detail    {}
                                                                                                                              :id        {}
                                                                                                                              :post-date {}
                                                                                                                              :title     {}}
                                                       #{:service.types/credit-card-balances} #:service.credit-card-balances {:available {}
                                                                                                                              :due       {}
                                                                                                                              :future    {}
                                                                                                                              :open      {}
                                                                                                                              :prepaid   {}}
                                                       #{:service.types/customer}             #:service.customer             {:cpf                 {}
                                                                                                                              :credit-card-account #:service.types      {:credit-card-account {}}
                                                                                                                              :feed                #:service.interfaces {:feed-event {}}
                                                                                                                              :id                  {}
                                                                                                                              :name                {}
                                                                                                                              :preferred-name      {}
                                                                                                                              :savings-account     #:service.types      {:savings-account {}}}
                                                       #{:service.types/onboarding-event}     {:service.onboarding-event/detail    {}
                                                                                               :service.onboarding-event/id        {}
                                                                                               :service.onboarding-event/post-date {}
                                                                                               :service.onboarding-event/title     {}
                                                                                               :service.interfaces/feed-event      {}}
                                                       #{:service.types/repository}           #:service.repository           {:id   {}
                                                                                                                              :name {}}
                                                       #{}                                    #:service                      {:banks               #:service.types {:bank {}}
                                                                                                                              :credit-card-account #:service.types {:credit-card-account {}}
                                                                                                                              :customer            #:service.types {:customer {}}
                                                                                                                              :repository          #:service.types {:repository {}}
                                                                                                                              :savings-account     #:service.types {:savings-account {}}
                                                                                                                              :viewer              #:service.types {:customer {}}}}
    :com.wsscode.pathom.connect/index-mutations       {com.wsscode.pathom.connect.graphql.service-mutations/service #:com.wsscode.pathom.connect {:sym com.wsscode.pathom.connect.graphql.service-mutations/service}
                                                       service/add-star                                             #:com.wsscode.pathom.connect {:sym com.wsscode.pathom.connect.graphql.service-mutations/service}
                                                       service/remove-star                                          #:com.wsscode.pathom.connect {:sym com.wsscode.pathom.connect.graphql.service-mutations/service}
                                                       service/request-reviews                                      #:com.wsscode.pathom.connect {:sym com.wsscode.pathom.connect.graphql.service-mutations/service}}
    :com.wsscode.pathom.connect/index-oir             {:service.customer/cpf                 {#{:service.customer/id} #{com.wsscode.pathom.connect.graphql2-test/supposed-resolver}}
                                                       :service.customer/credit-card-account {#{:service.customer/id} #{com.wsscode.pathom.connect.graphql2-test/supposed-resolver}}
                                                       :service.customer/feed                {#{:service.customer/id} #{com.wsscode.pathom.connect.graphql2-test/supposed-resolver}}
                                                       :service.customer/id                  {#{:service.customer/id} #{com.wsscode.pathom.connect.graphql2-test/supposed-resolver}}
                                                       :service.customer/name                {#{:service.customer/id} #{com.wsscode.pathom.connect.graphql2-test/supposed-resolver}}
                                                       :service.customer/preferred-name      {#{:service.customer/id} #{com.wsscode.pathom.connect.graphql2-test/supposed-resolver}}
                                                       :service.customer/savings-account     {#{:service.customer/id} #{com.wsscode.pathom.connect.graphql2-test/supposed-resolver}}
                                                       :service.repository/id                {#{:service.customer/name
                                                                                                :service.repository/name} #{com.wsscode.pathom.connect.graphql2-test/supposed-resolver}}
                                                       :service.repository/name              {#{:service.customer/name
                                                                                                :service.repository/name} #{com.wsscode.pathom.connect.graphql2-test/supposed-resolver}}
                                                       :service/banks                        {#{} #{com.wsscode.pathom.connect.graphql2-test/supposed-resolver}}
                                                       :service/credit-card-account          {#{} #{com.wsscode.pathom.connect.graphql2-test/supposed-resolver}}
                                                       :service/customer                     {#{} #{com.wsscode.pathom.connect.graphql2-test/supposed-resolver}}
                                                       :service/repository                   {#{} #{com.wsscode.pathom.connect.graphql2-test/supposed-resolver}}
                                                       :service/savings-account              {#{} #{com.wsscode.pathom.connect.graphql2-test/supposed-resolver}}
                                                       :service/viewer                       {#{} #{com.wsscode.pathom.connect.graphql2-test/supposed-resolver}}}
    :com.wsscode.pathom.connect/index-resolvers       #:com.wsscode.pathom.connect.graphql2-test {supposed-resolver {:com.wsscode.pathom.connect.graphql2/graphql? true
                                                                                                                     :com.wsscode.pathom.connect/cache?            false
                                                                                                                     :com.wsscode.pathom.connect/sym               com.wsscode.pathom.connect.graphql2-test/supposed-resolver}}})

(deftest test-munged-index-schema
  (is (= (-> (pcg/index-schema {::pcg/prefix    prefix ::pcg/schema snake-schema
                                ::pcg/mung csk/->kebab-case
                                ::pcg/demung csk/->snake_case
                                ::pcg/ident-map {"customer"            {"customer-id" :service.customer/id}
                                                 "credit-card-account" {"customer-id" :service.customer/id}
                                                 "savings-account"     {"customer-id" :service.customer/id}
                                                 "repository"          {"owner" :service.customer/name
                                                                        "name"  :service.repository/name}}
                                ::pcg/resolver  `supposed-resolver})
             (update-in [::pc/index-resolvers `supposed-resolver] dissoc ::pc/compute-output ::pc/resolve)
             (update-in [::pc/index-mutations 'com.wsscode.pathom.connect.graphql.service-mutations/service] dissoc ::pc/mutate))
         munged-indexes)))

(deftest test-alias-for-line
  (is (= (pcg/alias-for-line "query { \ncustomer(customerId: \"123\") {\n}}" 2)
         nil))

  (is (= (pcg/alias-for-line "query { \n_customer_customer_id_123: customer(customerId: \"123\") {\n}}" 2)
         "_customer_customer_id_123"))

  (is (= (pcg/alias-for-line "query { \n_customer_customer_id_123: customer(customerId: \"123\") {\n}}" 10)
         nil)))

(deftest test-index-graphql-errors
  (is (= (pcg/index-graphql-errors
           [{:message   "Parse error on \"-\" (error) at [3 11]"
             :locations [{:line 3 :column 11}]}])
         {nil [{:message   "Parse error on \"-\" (error) at [3 11]"
                :locations [{:line 3 :column 11}]}]}))
  (is (= (pcg/index-graphql-errors
           [{:message "Forbidden"
             :path    ["didWrong"]}])
         {["didWrong"] [{:message "Forbidden", :path ["didWrong"]}]}))
  (is (= (pcg/index-graphql-errors
           [{:message "Forbidden"
             :path    ["query" "didWrong"]}])
         {["didWrong"] [{:message "Forbidden", :path ["didWrong"]}]}))
  (is (= (pcg/index-graphql-errors
           [{:path       ["mutation" "addStar" "clientMutation"]
             :extensions {:code      "undefinedField"
                          :typeName  "AddStarPayload"
                          :fieldName "clientMutation"}
             :locations  [{:line 3 :column 5}]
             :message    "Field 'clientMutation' doesn't exist on type 'AddStarPayload'"}])
         {["addStar" "clientMutation"] [{:path       ["addStar" "clientMutation"]
                                         :extensions {:code      "undefinedField"
                                                      :typeName  "AddStarPayload"
                                                      :fieldName "clientMutation"}
                                         :locations  [{:line 3 :column 5}]
                                         :message    "Field 'clientMutation' doesn't exist on type 'AddStarPayload'"}]})))

(deftest test-parse-item
  (is (= (pcg/parser-item {::p/entity {}} [])
         {}))
  (is (= (pcg/parser-item {::p/entity {:itemValue 42}
                           ::pcg/demung pg/camel-case}
           [:ns/item-value])
         {:ns/item-value 42}))
  (is (= (pcg/parser-item {::p/entity               {:itemValue {:x 1 :y 2}}
                           ::p/placeholder-prefixes #{">"}}
           [{:itemValue [:x {:>/sub [:y]}]}])
         {:itemValue {:x 1 :>/sub {:y 2}}}))
  (is (= (pcg/parser-item {::p/entity   {:didWrong nil}
                           ::pcg/demung pg/camel-case
                           ::pcg/errors (pcg/index-graphql-errors
                                          [{:message "Forbidden"
                                            :path    ["didWrong"]}])}
           [{:did-wrong [:anything]}])
         {:did-wrong ::p/reader-error}))
  (testing "capture error"
    (let [errors* (atom {})]
      (is (= (pcg/parser-item {::p/entity          {:_customer_customer_id_123 {:creditCardAccount nil}}
                               ::p/errors*         errors*
                               ::pcg/demung        pg/camel-case
                               ::pcg/base-path     [[:service.Customer/id "123"]]
                               ::pcg/graphql-query "query \n{_customer_customer_id_123: customer(customerId: \"123\") \n{}}"
                               ::pcg/errors        (pcg/index-graphql-errors [{:locations [{:column 123 :line 2}]
                                                                               :message   "Forbidden"
                                                                               :path      ["customer" "creditCardAccount"]
                                                                               :type      "forbidden"}])}
               [{[:customer/customerId "123"] [{:service.Customer/credit-card-account [:service.credit-card-balances/available]}]}])
             {[:customer/customerId "123"] {:service.Customer/credit-card-account ::p/reader-error}}))
      (is (= @errors*
             {[[:service.Customer/id "123"] :service.Customer/credit-card-account] {:locations [{:column 123 :line 2}]
                                                                                    :message   "Forbidden"
                                                                                    :path      ["customer" "creditCardAccount"]
                                                                                    :type      "forbidden"}}))))

  (testing "mutation errors"
    {:errors
     [{:path      ["query" "nameWithOwneree"],
       :extensions
                  {:code      "undefinedField",
                   :typeName  "Query",
                   :fieldName "nameWithOwneree"},
       :locations [{:line 7, :column 3}],
       :message
                  "Field 'nameWithOwneree' doesn't exist on type 'Query'"}]}

    {:errors
     [{:message   "Parse error on \"-\" (error) at [3 11]"
       :locations [{:line 3 :column 11}]}]}))

; TODO proper process mutation error responses

(comment
  (let [errors* (atom {})]
    [(pcg/parser-item {::p/entity          nil
                       ::p/errors*         errors*
                       ::pcg/base-path     []
                       ::pcg/graphql-query "query {\n  addStar(input: {starrableId: \"MDEwOlJlcG9zaXRvcnk5ODU5MDk2MQ==\"}) {\n    clientMutation\n    starrable {\n      viewerHasStarred\n    }\n  }\n}"
                       ::pcg/errors        (pcg/index-graphql-errors [{:path       ["mutation" "addStar" "clientMutation"]
                                                                       :extensions {:code      "undefinedField"
                                                                                    :typeName  "AddStarPayload"
                                                                                    :fieldName "clientMutation"}
                                                                       :locations  [{:line 3 :column 5}]
                                                                       :message    "Field 'clientMutation' doesn't exist on type 'AddStarPayload'"}])}
       '[{(:github/addStar
            {:github/input
             {:github/starrableId "MDEwOlJlcG9zaXRvcnk5ODU5MDk2MQ=="}})
          [:clientMutation {:starrable [:viewerHasStarred]}]}])
     @errors*])

  (let [errors* (atom {})]
    [(pcg/parser-item {::p/entity          nil
                       ::p/errors*         errors*
                       ::pcg/base-path     []
                       ::pcg/graphql-query "query {\n  addStar(input: {starrableId: \"MDEwOlJlcG9zaXRvcnk5ODU5MDk2MQ==\"}) {\n    clientMutation\n    starrable {\n      viewerHasStarred\n    }\n  }\n}"
                       ::pcg/errors        (pcg/index-graphql-errors
                                             [{:message   "Parse error on \"-\" (error) at [3 11]"
                                               :locations [{:line 3 :column 11}]}])}
       '[{(:github/addStar
            {:github/input
             {:github/starrableId "MDEwOlJlcG9zaXRvcnk5ODU5MDk2MQ=="}})
          [:clientMutation {:starrable [:viewerHasStarred]}]}])
     @errors*]))

(comment
  (println
    (pcg/query->graphql '[{(:github/addStar
                             {:github/input
                              {:github/starrableId "MDEwOlJlcG9zaXRvcnk5ODU5MDk2MQ=="}})
                           [:clientMutation {:starrable [:viewerHasStarred]}]}]
      {})))

(deftest test-query->graphql
  (is (= (pcg/query->graphql [{:credit-card [:number]}] {::pcg/demung pg/camel-case})
         "query {\n  creditCard {\n    number\n  }\n}\n")))

(defn q [query]
  (p/query->ast1 [query]))

(deftest test-ast->graphql
  (is (= (pcg/ast->graphql {:ast         (q :service/banks)
                            ::pc/indexes indexes} {})
         [:service/banks]))
  (is (= (pcg/ast->graphql {:ast         (q {:service/banks [:service.Bank/name]})
                            ::pc/indexes indexes} {})
         [{:service/banks [:service.Bank/name]}]))
  (is (= (pcg/ast->graphql {:ast         (q :service.Customer/cpf)
                            ::pc/indexes indexes}
           {:service.Customer/id "123"})
         [{[:customer/customerId
            "123"] [:service.Customer/cpf]}])))

(defn query-env [query-attribute entity]
  {:ast                     (q query-attribute)
   ::p/entity               entity
   ::p/placeholder-prefixes #{">"}
   ::p/parent-query         [query-attribute]
   ::pcg/prefix             prefix
   ::pc/indexes             indexes})

(deftest test-build-query
  (testing "build global attribute"
    (is (= (pcg/build-query (query-env :service/banks
                              {:service.Customer/id "123"}))
           [:service/banks])))

  (testing "remove pathom params"
    (is (= (pcg/build-query (query-env '(:service/banks {:pathom/as :banks})
                              {:service.Customer/id "123"}))
           ['(:service/banks)])))

  (testing "ident join"
    (is (= (pcg/build-query (query-env :service.Customer/cpf
                              {:service.Customer/id "123"}))
           [{[:customer/customerId "123"] [:service.Customer/cpf]}])))

  (testing "ident join on multi param input"
    (is (= (pcg/build-query (query-env :service.Repository/id
                              {:service.Customer/name   "customer"
                               :service.Repository/name "repository"}))
           [{[:repository/owner-and-name ["customer" "repository"]] [:service.Repository/id]}])))

  (testing "ignores ident queries"
    (is (= (pcg/build-query (query-env {[:service.Customer/id "123"] [:service.Customer/name]}
                              {:service.Customer/id "123"}))
           [])))

  (testing "merge sibling queries"
    (is (= (pcg/build-query (assoc (query-env :service.Customer/id {:service.Customer/id "123"})
                              ::p/parent-query [:service.Customer/id
                                                :service.Customer/cpf
                                                :service/banks
                                                :service.Customer/name
                                                :other/thing]))
           [{[:customer/customerId "123"] [:service.Customer/cpf :service.Customer/name]}
            :service/banks])))

  (testing "placeholder queries"
    (is (= (pcg/build-query (assoc (query-env :service.Customer/id {:service.Customer/id "123"})
                              ::p/parent-query [:service.Customer/id
                                                {:>/thing [:service.Customer/cpf]}
                                                :service/banks
                                                :service.Customer/name
                                                :other/thing]))
           [:service/banks
            {[:customer/customerId "123"] [:service.Customer/name :service.Customer/cpf]}]))))

(deftest test-pull-idents
  (is (= (pcg/pull-idents {:service/banks                [{:service.Bank/name "Dino"}]
                           [:customer/customerId "123"] {:service.Customer/name "Missy"}})
         {:service/banks         [{:service.Bank/name "Dino"}]
          :service.Customer/name "Missy"})))

