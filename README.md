# pdf-easy-image-remover

Remove images (especially background images) from all pages of a pdf.

## Motivation

In my opinion the top priority for any PDF should be readability. But sadly many PDFs come
with distracting background images or patterns behind the text which make them unpleasant
to read (and often are also ugly).

To my knowledge there are no free to use editors available to remove such images. This
software uses the excellent (but complicated) [itext7](https://github.com/itext/itext7) library
to remove any images you want from any given PDF.

## Usage

First extract images using any page that contains the image you want to remove:

```
java -jar pdf-easy-image-remover-1.0.jar <extract|x> <input-file> <page-number>
```

This will create a sub folder "images" and put all images that are on the given
page in this folder. The filenames are `<hash-of-image>.png`.

Look through the files and find the ones you want to remove and then call:

```
java -jar pdf-easy-image-remover-1.0.jar <remove|r> <input-file> <output-file> <image-hash ...>
```

The output file will contain a copy of the original PDF with all given images removed from every page.
The actual image data is still contained in the file, but it is no longer rendered anywhere.
