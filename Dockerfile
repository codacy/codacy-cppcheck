# dev is just used for development purposes. Not used for production images
FROM codacy-cppcheck-base as dev

RUN apk add openjdk17
COPY docs /docs	
COPY addons/misra* /workdir/addons/	
RUN adduser --uid 2004 --disabled-password --gecos "" docker	
COPY target/universal/stage/ /workdir/	
RUN chmod +x /workdir/bin/codacy-cppcheck	
USER docker	
WORKDIR /workdir
ENTRYPOINT ["bin/codacy-cppcheck"]

FROM sbtscala/scala-sbt:graalvm-ce-22.3.3-b1-java17_1.9.9_3.4.0 AS builder

WORKDIR /workdir
COPY . .
RUN sbt nativeImage

FROM codacy-cppcheck-base

COPY docs /docs
COPY addons/misra* /workdir/addons/
RUN adduser --uid 2004 --disabled-password --gecos "" docker
COPY --from=builder /workdir/target/native-image/codacy-cppcheck /workdir/
USER docker
WORKDIR /workdir
ENTRYPOINT ["/workdir/codacy-cppcheck"]
