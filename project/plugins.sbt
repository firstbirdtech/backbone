addSbtPlugin("com.geirsson"  % "sbt-scalafmt"        % "0.5.6")
addSbtPlugin("org.scoverage" % "sbt-scoverage"       % "1.5.0")
addSbtPlugin("com.codacy"    % "sbt-codacy-coverage" % "1.3.8")
addSbtPlugin("com.dwijnand"  % "sbt-dynver"          % "1.2.0")
addSbtPlugin("me.lessis"     % "bintray-sbt"         % "0.3.0-8-g6d0c3f8")
addSbtPlugin("com.eed3si9n"  % "sbt-doge"            % "0.1.5")

resolvers += Resolver.url("2m-sbt-plugin-releases", url("https://dl.bintray.com/2m/sbt-plugin-releases/"))(
  Resolver.ivyStylePatterns)
