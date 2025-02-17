(ns pingcrm.models.contacts
  (:require [honey.sql :as h]
            [honey.sql.helpers :refer [where]]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]))

(defn count-contacts [db]
  (let [query (h/format {:select [[:%count.* :aggregate]]
                         :from [:contacts]
                         :where [:and
                                 [:= :account_id 1]
                                 [:<> :account_id nil]
                                 [:= :deleted_at nil]]})]
    (jdbc/execute-one! db query)))

(defn retrieve-and-filter-contacts
  [db {:keys [search trashed]} offset]
  ;;TODO: Add filter by organization name
  (let [query (h/format
               (cond-> {:select [[:c.*] [[:|| :c.first_name " " :c.last_name] :name] [:o.name :organization]]
                        :from [[:contacts :c]]
                        :left-join [[:organizations :o] [:= :c.organization_id :o.id]]
                        :order-by [:last_name :first_name]
                        :limit 10
                        :offset offset}
                 search (where [:or [:like :c.first_name (str "%" search "%")]
                                [:like :c.last_name (str "%" search "%")]
                                [:like :c.email (str "%" search "%")]])
                 true (where (case trashed
                                  "with" nil
                                  "only" [:<> :c.deleted_at nil]
                                  [:= :c.deleted_at nil]))))]
    (jdbc/execute! db query)))

(defn get-contact-by-id
  [db id]
  (sql/get-by-id db :contacts id))

(defn insert-contact!
  [db contact]
  (let [query (h/format{:insert-into :contacts
                        :values [(merge contact {:created_at :current_timestamp
                                                 :updated_at :current_timestamp})]})]
    (jdbc/execute-one! db query)))

(defn update-contact!
  [db contact id]
  (sql/update! db :contacts contact {:id id}))

(defn soft-delete-contact!
  [db id]
  (let [query (h/format {:update :contacts
                         :set {:deleted_at :current_timestamp
                               :updated_at :current_timestamp}
                         :where [:= :id id]})]
    (jdbc/execute-one! db query)))

(defn restore-deleted-contact!
  [db id]
  (let [query (h/format {:update :contacts
                         :set {:deleted_at nil
                               :updated_at :current_timestamp}
                         :where [:= :id id]})]
    (jdbc/execute-one! db query)))
