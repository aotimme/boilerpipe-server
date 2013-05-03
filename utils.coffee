debug   = require 'debug'
async   = require 'async'
phantom = require 'phantom'
request = require('request').defaults jar: false

BOILERPIPE_URL = 'http://localhost:6666'
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
  ], (err, res, body) ->
    callback err, body ? null
