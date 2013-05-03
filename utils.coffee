debug   = require 'debug'
async   = require 'async'
phantom = require 'phantom'
request = require('request').defaults jar: false

BOILERPIPE_URL = 'http://localhost:6666'
NER_URL = 'http://localhost:6664'
TIMEOUT = 1000 * 10   # timeout after 10 seconds

debug = debug 'extractor:utils'

getHTMLForUrl = (url, callback) ->
  # request and zombiejs didn't work :(
  phantom.create (ph) ->
    ph.createPage (page) ->
      hasCalledBack = false
      doCallback = ->
        return if hasCalledBack
        debug 'TIMEOUT!', url
        ph.exit()
        callback null, null
      setTimeout doCallback, 30 * 1000    # quit in 30 seconds

      # set settings.userAgent?
      page.set 'viewportSize', width: 1286, height: 828
      page.open url, (status) ->
        page.evaluate (-> document.getElementsByTagName('html')[0].innerHTML)
        , (html) ->
          return if hasCalledBack
          hasCalledBack = true
          ph.exit()
          callback null, html

exports.getDataForUrl = (url, callback) ->
  data = null
  async.waterfall [
    (next) ->
      getHTMLForUrl url, next
    (html, next) ->
      request
        method: 'GET'
        url: BOILERPIPE_URL
        body: html
        timeout: TIMEOUT
        json: true
      , next
    (res, body, next) ->
      data = body
      request
        method: 'POST'
        url: NER_URL
        json: text: data.content
      , next
        # add `entities` to data after getting response
  ], (err, res, body) ->
    return callback err if err
    data.entities = body.entities
    callback null, data
