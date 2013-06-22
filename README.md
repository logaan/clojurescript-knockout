# event-thread

A Clojure library designed to ... well, that part is up to you.

## Usage

FIXME

## License

Copyright © 2013 FIXME

Distributed under the Eclipse Public License, the same as Clojure.

So lets just keep it simple at start without it being a seq of futures. How do
you deal with the first value. Because you need to be able to hand someone the
stream before any events have been fired.

So then first needs to take a callback. But what if instead first returned
immediately but returned a future.  So then what does rest do. Well it could
take a callback... or it could immediately return a new cell with a future for
first and this same behavior for rest.

But we need to make sure that if you call rest again that you get the same next
value. So we'll have to have rest mutate the cell so that it knows not to try
and generate a new tail. So then how do you add values to the seq?  Well you
have some ref that's pointing to the tail most cell. When you want to add
something

1. You call rest
2. Update the atom to point at the value of rest
2. Get the future from the head of the new cell
3. Deliver on the value to the future

Then anyone who has an earlier cell will be fine because the end of their list
has just updated it's self to know not to generate a new tail. So when they
eventually request the tail they'll get the same one.

The problem is that there's no way to say that the stream is dead. But I'm not
sure if that's a huge concern.
