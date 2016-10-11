# mut.music

This module implements tools for describing music.

When people try to describe and analyze music, they define a musical notation
like the tabulature, or letters or so called 'Modern Notation'.
These notations are good for humans, but not that good for computers.
So this module defines its own notation which is good for use with clojure.

Here we define such things as Beat, Duration, Pitch, Note, Interval, Chord, etc.
All these things (we call them "musical objects") are building blocks for music.
You can compose songs from musical objects defined in this module.

However, this module doesn't compose music (it doesn't have functions that
that involve random generators or something), and it doesn't play the music
(it doesn't do any I/O); generally it tries to avoid any non-pure functions.

We like to think of this module (`mut.music`) as a library of musical objects.
It defines various types of musical objects, and it defines some concepts of the
music theory, but it doesn't apply these things to anything.
The real work happens in higher-level modules who use this library to compose
musical objects together into something sensible.
