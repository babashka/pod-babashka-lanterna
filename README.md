# pod-babashka-lanterna

A pod exposing [clojure-lanterna](https://github.com/babashka/clojure-lanterna).

## API

The following namespaces + vars are exposed:

- `pod.babashka.lanterna.terminal`:
  - `get-terminal`
  - `start`
  - `stop`
  - `put-string`
  - `flush`
  - `get-key`
  - `get-key-blocking`
  - `get-size`
- `pod.babashka.lanterna.screen`:
  - `put-string`
  - `redraw`
  - `get-key`
  - `get-key-blocking`
  - `start`
  - `stop`
  - `get-size`

More can be added, PRs welcome.

## Examples

- Run `bb lanterna.cljc` for a simple demo.
- [console-tetris](https://github.com/borkdude/console-tetris):

  ``` clojure
  $ export BABASHKA_CLASSPATH=$(clojure -Spath -Sdeps '{:deps {borkdude/console-tetris {:git/url "https://github.com/borkdude/console-tetris" :deps/manifest :deps :sha "f2b8388b160b0326d72a7f18785687175910504f"}}}')
  $ bb -m tetris.core
  ```

  Note: this will be considerably slower than the GraalVM tetris from the same repo.

## Dev

### Build

Run `script/compile`

### Test

Run `bb lanterna.cljc`.

## License

Copyright © 2020 Michiel Borkent

Distributed under the EPL License. See LICENSE.

Clojure-lanterna and Lanterna are distributed under the GNU Lesser General Public License.
