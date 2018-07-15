## Synopsis

**az0t** is a program written in java which allows automatic login and reconnection to zeroshell.

## Prerequesties

In order to work, **az0t** requires a fully functionnal java runtime environment.
You can grab one [here](https://www.java.com/)

Compiled binaries are available in *az0t.zip*

## Editing configuration

Edit the configuration with your username and password

```shell
$ vim config.xml

<root>
<username>example</username>                                    # your username
<password>example</password>                                    # your password
<zeroshell>http://192.168.0.1:12082/cgi-bin/zscp</zeroshell>    # captive portal address
<delay>40</delay>                                               # delay between each request
<realm>arpej.com</realm>                                        # domain of your zeroshell
<section>CPGW</section>                                         # section parameter
</root>
```

## Usage


```shell
$ java -jar az0t.jar -c    # generate default configuration file
$ java -jar az0t.jar -h    # show help message
$ java -jar az0t.jar -m    # start the monitor mode

```


## About sources

The sources are available under the GNU-GPL v3.0 license. 
Howewer, this program use the jsoup library under the MIT license.

In case of bug, contact me via email, I will answer you ASAP.
