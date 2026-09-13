# DiaBAD upload key

`diabad-upload.jks` signs debug and release APKs so in-app updates
keep the same certificate across cloud-agent builds.

Do not replace this file: a new key cannot overlay an already installed
`com.diabad` package.
