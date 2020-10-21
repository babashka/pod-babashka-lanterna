# pod-babashka-lanterna

WIP. Babashka pods need to implement sockets in addition to stdin/stdout, since clojure-lanterna writes to stdout as well.
See https://github.com/babashka/babashka.pods/issues/2.

A stretch goal: get the `pod-babashka-lanterna` branch of [console-tetris]() working:

``` clojure
$ export BABASHKA_CLASSPATH=$(clojure -Spath -Sdeps '{:deps {borkdude/console-tetris {:git/url "https://github.com/borkdude/console-tetris" :deps/manifest :deps :sha "53016bfb85f839aa7e2465cc51c1cb749dc1c240"}}}')
$ bb -m tetris.core
```

## Dev

### Build

Run `script/compile`

### Test

Run `bb lanterna.clj`.

## License

Copyright © 2020 Michiel Borkent

Distributed under the EPL License. See LICENSE.

Clojure-lanterna and Lanterna are distributed under the GNU Lesser General Public License.
