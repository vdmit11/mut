(ns mut.music.instr
  "Tools for working with instruments in music score.")

(defn- remove-kw-digits-suffix [kw]
  (let [str (name kw)
        [_ str-without-suffix] (re-find #"^(.+)-\d+$" str)
        kw-without-suffix (keyword str-without-suffix)]
    (or kw-without-suffix kw)))

(defn id->type
  "Extract type from instrument ID keyword.

  Examples:
     (id->type :electric-guitar) => :electric-guitar
     (id->type :electric-guitar-1) => :electric-guitar
     (id->type :electric-guitar-2) => :electric-guitar

  Basically that removes the number from the end of the keyword.

  But the sense of this operation is that the input keyword is instrument ID,
  used in a musical score. And we define it to have the following format:

    :[TYPE]-[NUMBER]

  So for example `:guitar-1` can be split to `:guitar` (type) and `1` (number of the instrument).

  This is a bit silly from programmer's point of view, but this is the usual way for wirting
  musical score, like saying 'Gutar #1 plays this part, and Guitar #2 plays that part'.
  And that works for us (and we're free to switch to something more complex in future if needed).

  So essentially, this function extracts the instrument type that is encoded inside ID keyword.
  "
  [id-kw]
  (remove-kw-digits-suffix id-kw))
