#!/usr/bin/env python

import nltk

from flask import Flask, request, jsonify
app = Flask(__name__)

WARMUP_TEXT = "Alden lives in Palo Alto, in California."

def extract_entities(text):
  result = dict()
  for sent in nltk.sent_tokenize(text):
    for chunk in nltk.ne_chunk(nltk.pos_tag(nltk.word_tokenize(sent))):
      if hasattr(chunk, 'node'):
        if chunk.node not in result:
          result[chunk.node] = dict()
        value = ' '.join(c[0] for c in chunk.leaves())
        if value not in result[chunk.node]:
          result[chunk.node][value] = 0
        result[chunk.node][value] += 1
  return result

@app.route("/", methods=['POST'])
def index():
  text = request.json['text']
  if not text:
    return 400
  return jsonify(entities=extract_entities(text))

if __name__ == "__main__":
  print extract_entities(WARMUP_TEXT)
  print "Done!"
  app.run(port=6664)
