debug   = require 'debug'
async   = require 'async'
_       = require 'underscore'
phantom = require 'phantom'
request = require('request').defaults jar: false

BOILERPIPE_URL = 'http://localhost:6664'
NER_URL = 'http://localhost:6665'
TIMEOUT = 1000 * 10   # timeout after 10 seconds

debug = debug 'extractor:utils'

URL_REGEXP = /\b((?:https?:\/\/|www\d{0,3}[.]|[a-z0-9.\-]+[.][a-z]{2,4}\/?)(?:[^\s()<>]+|\(([^\s()<>]+|(\([^\s()<>]+\)))*\))*(?:\(([^\s()<>]+|(\([^\s()<>]+\)))*\)|[^\s`!()\[\]{};:'".,<>?«»“”‘’]?))/ig

#getHTMLForUrl = (url, callback) ->
#  # request and zombiejs didn't work :(
#  phantom.create (ph) ->
#    ph.createPage (page) ->
#      hasCalledBack = false
#      doCallback = ->
#        return if hasCalledBack
#        debug 'TIMEOUT!', url
#        ph.exit()
#        callback null, null
#      setTimeout doCallback, 30 * 1000    # quit in 30 seconds
#
#      # set settings.userAgent?
#      page.set 'viewportSize', width: 1286, height: 828
#      page.open url, (status) ->
#        page.evaluate (-> document.getElementsByTagName('html')[0].innerHTML)
#        , (html) ->
#          return if hasCalledBack
#          hasCalledBack = true
#          ph.exit()
#          callback null, html
getHTMLForUrl = (url, callback) ->
  request url, (err, res, body) ->
    callback err, url: res?.request?.uri?.href, html: body

exports.getDataForUrl = (url, callback) ->
  data = original_url: url
  async.waterfall [
    (next) ->
      getHTMLForUrl url, next
    ({url, html}, next) ->
      data.url = url
      request
        method: 'GET'
        url: BOILERPIPE_URL
        body: html
        timeout: TIMEOUT
        json: true
      , next
    (res, body, next) ->
      data = _(data).extend body
      request
        method: 'POST'
        url: NER_URL
        json: text: data.content
      , next
  ], (err, res, body) ->
    return callback err if err
    # add `entities` to data after getting response
    data.entities = body.entities
    data.url_mentions = data.content.match(URL_REGEXP) ? []
    callback null, data
