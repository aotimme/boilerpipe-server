express = require 'express'
debug   = require 'debug'

utils = require './utils'

debug = debug 'extractor:server'

PORT = 6667

app = express()

getHTMLForUrl = (url, callback) ->

app.get '/', (req, res, next) ->
  url = req.query.url
  utils.getDataForUrl url, (err, results) ->
    return next err if err
    res.json results

app.listen PORT

module.exports = app
