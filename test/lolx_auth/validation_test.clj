(ns lolx-auth.validation-test
  (:use midje.sweet)
  (:require
   [lolx-auth.validation :refer :all]))


(fact "returns true for all not-empty values"
  (registration-valid? "john" "deer" "deer@wp.pl" "23 323 23 " "pass" "location" ) => true)

(fact "returns false if last-name is empty"
  (registration-valid? "john" "" "deer@wp.pl" "2323 23 23 2 " "pass" "location") => false)
