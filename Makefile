repl:
	screen -t "cljs repl" lein trampoline cljsbuild repl-rhino
watch:
	screen -t "cljs autobuild" lein cljsbuild auto
