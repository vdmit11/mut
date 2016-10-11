# mut.music.properties

This module defines such things as Duration, Volume, Pitch.

These things are not very interesting on they own, usually they act as
properties of various musical objects.

For example, we can say that:
 - a note has Pitch, Duration and Volume
 - a chord also has Pitch (the root of the chord), and has Duration and Volume
 - a rest (pause) has Duration, but doesn't have Pitch and Volume
 - a song as a whole has Duration, and maybe has Volume

So we use the term "properties" to group these things together.
We say that these things (Pitch, Duration, Volume, and other similar things)
are perceptual properties of sound.


## Properties are separate objects

These properties (Pitch, Volume, etc) don't really exist as physical objects.
They are rather abstract things in your head.

You can't hear a Pitch, and you can't really play Pitch on guitar, but can hear
a sound that *has* a Pitch, and you probably can tell something like
"a-ha, these two sounds have different Pitches".

And you can visualize pitches, order them, and build ranges of pitches.
So we can conclude that a pitch is actually an object, kinda second-class
citizen, but still more than just a dumb attribute of something.

And we can see that all properties are such objects, so this module contains
`(deftype)`s for them.

So for example instead of representing Volume as a number, we do
`(deftype Volume)` and work with volumes as objects of separate type.

That may seem strange at first (why don't we just use numbers to represent
the volume, huh?), but it becomes more beneficial to have separate types
later, when you start extending these types with new interfaces and protocols.

(Of couse, in Clojure, you can extend the Number class with some protocol, but
what if you need to implement the protocol differently for Volume and Duration?
You get into trouble, because you need to provide two different implementations
for the same method of the Number class.
So it is better to define separate classes instead of just using numbers.)


## Properties don't refer to the music theory

This module defines the `Pitch` for example.

But it is important to understand, that this Pitch doesn't "know" anything
about diatonic scales, or pitch classes of the musical set theory.

Yes, in this module we use the Scientific Pitch Notation for representing
pitches as strings (e.g. "C3", "A#5", "Eb4", etc), and the historical origin of
this is the classical music theory. But this is only notation, and it is
important to understand, that in this module we don't apply music theory
to compose anything.

So Pitch is a more low-level thing, and the music theory is built on top of it.
And this is true for all other properties defined in this module.

