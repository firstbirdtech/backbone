# Releasing

To release a new version of backbone follow the following steps:

1. Make sure all changes are commited to the master branch and that the builds successfully passed 
2. Use git to tag the lastest master commit with the new version: `git tag -a vx.x.x -m "vx.x.x"`
3. Push local git tag to remote using `git push --follow-tags`
4. Wait until [GitHub Action](https://github.com/firstbirdtech/backbone/actions?query=workflow%3ACI) finished the publish job
5. Create a new release in [GitHub](https://github.com/firstbirdtech/backbone/releases)