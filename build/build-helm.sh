docker run -v /var/run/docker.sock:/var/run/docker.sock -v $(pwd):/src alpine/helm:3.2.3 package /src/$1 --app-version $2 --version $2 -d /src

