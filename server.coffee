{spawn} = require 'child_process'

javaServer = spawn './run_server.sh'
javaServer.stdout.on 'data', (data) ->
  console.log data.toString().trim()
javaServer.stderr.on 'data', (data) ->
  console.log data.toString().trim()
javaServer.on 'close', (code) ->
  console.log "JAVA CLOSE: #{code}"

pythonServer = spawn './nltk-ner.py'
pythonServer.stdout.on 'data', (data) ->
  console.log data.toString().trim()
pythonServer.stderr.on 'data', (data) ->
  console.log data.toString().trim()
pythonServer.on 'close', (code) ->
  console.log "PYTHON CLOSE: #{code}"


env = process.env
env.DEBUG = '*'
nodeServer = spawn 'node_modules/.bin/coffee', ['app.coffee'], {env}
nodeServer.stdout.on 'data', (data) ->
  console.log data.toString().trim()
nodeServer.stderr.on 'data', (data) ->
  console.log data.toString().trim()
nodeServer.on 'close', (code) ->
  console.log "NODE CLOSE: #{code}"
