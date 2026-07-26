# SPnuts release site

`index.html` is the source for the GitHub Pages release site.  It uses the
`{{VERSION}}` placeholder, replaced only by `scripts/build-pages.sh`.  The
site builder copies the tested release assets and checksum file into the
published `downloads/` directory; do not hand-copy release archives here.
