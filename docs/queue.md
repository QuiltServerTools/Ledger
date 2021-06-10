# Queue
Whenever an action happens, it is added to the database queue.
Often, the queue will be empty, and it will be instantly inserted into the database.
If there is lots of players or something like an explosion creating lots of actions,
the queue may fill up and it could take a while for actions to be logged.  
You can see the status of your database queue by using the [status command](commands/status.md).
If the server stops while there is still actions  in the queue that need to be logged,
it will force the server to wait until it is finished logging the actions or until it times out.
You can adjust the timeout in the [config](config.md#database-settings)