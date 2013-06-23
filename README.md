# Event thread

Here I have implemented asynchronous lists. They server the same purpose as
blocking lazy seqs do in Cloure. But because javascript is single threaded we
can't block, so we have to be async.

The list will appear to the outside world like any other sequence. However all
values will be deferred. To get the actual value you must use jayq.core/done to
register a callback. You can operate on the list even before values exist, so
it's safe to say (first (rest (rest alist))) to get a deferred that represents
the 3rd value that will exist in the alist.

There are three levels of abstraction. The lower is the
[cell](/src-cljs/event_thread/cell.cljs). A cell is just like a list cons cell.
Ontop of cells are build [deferred cells](/src-cljs/event_thread/dcell.cljs).
They are a cell wrapped is a deferred that has a deferred cell for rest. First
in a deferred cell will be an actual value. At the top we have
[aseqs](/src-cljs/event_thread/dlist.cljs), some of which will have a writer
atom. Using the writer atom you can inject values into the tail of the
sequence. Consumers of the alist (the reader part) will only ever see a
functional interface.

--------

All the good stuff is in
[`src-cljs/event_thread/aseq.cljs`]
(/src-cljs/event\_thread/aseq.cljs)

Event thread is an attempt at making a functional reactive library. Reactive
libraries already exist, things like Backbone and Knockout.js. There are even
some (mostly) functional ones like Bacon.js. This will bring the goodness to
Clojurescript. Clojurescript will let us use macros to hide any continuation
passing style code.

## Design Notes

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

----------

So it's kind of done-ish now. So... if we change it to wrap the cell in a
delegate. Then we have a legit seq, but it only ever returns futures... But we
can't have that because things are going to expect rest to return something
that we can get values directly out of. Which is ok, but we need to know when
not to give any more rest.... Hmm perhaps the current way is best.


----------

So... what if it's just a future wrapped around a cons cell?

Means you can:

### Close the seq without producing any values

1. Create an aseq (future wrapping cons)
2. Have someone deref the future
3. You close the seq
4. The deref resolves
5. They call first and receive nil

But can't comply with the seq interface because you can't call first on a
future without passsing in a callback.

So instead we need a cons cell with a future for first and an automatically
generated list with another future.

The problem here is that if you map over it then it'll just keep going
forever.

So we have first and rest both return futures... but we don't need first to
because by the time we've dereffed rest the thing that cause that to succeed
was the fact that we've got a value. So we're back to future wrapped around
cell again. And there's no way that it'll support seq because everything
needs rest to return a collection.

So... you could just have some onValue function that gets called every time
we have a value. But Then you've got no way of referring to the rest of the
values that you haven't consumed yet... But if we generate the 'rest of the
values' list before we know whether it's the end of the list or not then
we'll infinitely generate without any blocking. So we need a callback that
will give us the rest of the list. But passing the callback as a param to
rest is going to be a bit of a pain when we're going to monads. But when you
start the list it might have no first value... so you could have the
constructor return a future for a list.... you can wait for the list... call
first and you get it straight away, no future... but if you call rest then
you get another future wrapped around the rest of the list. This should solve
the timing problems. But we could just implement first and rest for the
future... they'll let you know when the future resolves and you could call
first to get your value directly... but if you're going to do that any way
then basically you just end up with first and rest both returning futures. So
they'll have the same interface as ISeq but the resturn value of rest won't
match up. So we won't be able to use the default map or anything but... maybe
that's the best we can have.

I guess the structure of a future wrapped around a cell would actually be
simpler than the map with two keys. It would mean that the value returned
from rest is actually a 'collection'.
