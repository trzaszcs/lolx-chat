(ns lolx-auth.validation)

(defn nil-or-empty? 
  [str]
  (or (nil? str) (empty? str))
)

(defn not-valid?
  [first-name last-name email password location]
  (or (nil-or-empty? first-name)
      (nil-or-empty? last-name)
      (nil-or-empty? email)
      (nil-or-empty? password)
      (nil? location))
)

(defn- all-not-empty?
  [list]
  (every?
   #(not (nil-or-empty? %))
   list))

(defn authroization-valid? 
  [email password]
  (all-not-empty? [email password]))

(defn registration-valid? 
  [first-name last-name email phone  password location]
  (all-not-empty? [first-name last-name email password location phone]))

(defn update-account-valid? 
    [email phone first-name last-name location]
    (all-not-empty?
     [email first-name last-name location phone]))

(defn change-password-valid? 
  [old-password new-password]
  (all-not-empty? [old-password new-password]))
