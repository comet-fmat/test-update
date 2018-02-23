# tmc-checkstyle-runner

[![Build Status](https://travis-ci.org/testmycode/tmc-checkstyle-runner.svg?branch=master)](https://travis-ci.org/testmycode/tmc-checkstyle-runner/)
[![Coverage Status](https://img.shields.io/coveralls/testmycode/tmc-checkstyle-runner.svg)](https://coveralls.io/r/testmycode/tmc-checkstyle-runner/)

tmc-checkstyle-runner is a stand-alone Java-software for running Checkstyle coding style validations on Java-code. It’s developed as an extension for [TMC](https://github.com/testmycode/), but also works separately. The extension is used in the [tmc-netbeans](https://github.com/testmycode/tmc-netbeans/) and [tmc-server](https://github.com/testmycode/tmc-server/) projects.

## Build

Build the project with `mvn clean package`. Install the dependency to your local Maven repository with `mvn clean install`.

## Test

Test the project with `mvn test`.

## Usage

Add the dependency to your project’s `pom.xml`. Exclude `checkstyle`-dependency to avoid translation files to be overwritten.

```xml
<dependency>
    <groupId>fi.helsinki.cs.tmc</groupId>
    <artifactId>tmc-checkstyle-runner</artifactId>
    <version>1.0.1</version>
    <exclusions>
        <exclusion>
            <groupId>com.puppycrawl.tools</groupId>
            <artifactId>checkstyle</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

Running Checkstyle validations can be accomplished programmatically or by running the software from the command-line.

### Programmatically

Create a `CheckstyleRunner` and pass the project directory to be tested as a file and the locale for validation messages to the constructor. You may also pass a `TMCCheckstyleConfiguration` as a third parameter to configure the runner. Running will return a `CheckstyleResult`. `CheckstyleResult`s implement the `ValidationResult`-interface.

```java
File projectDirectory = new File("path/to/project-directory/");
CheckstyleRunner runner = new CheckstyleRunner(projectDirectory, Locale.ENGLISH);
CheckstyleResult result = runner.run();
```

The API also provides methods for writing the results to a file as JSON or deserialising JSON-results as `CheckstyleResult`’s. See `CheckstyleResult.build(json)` and `result.writeToFile(file)`.

### CLI

Running Checkstyle validations from the command-line can be accomplished by passing the project directory path to be tested, output file path and locale (ISO 639) for validation messages as properties. The output file can be overwritten with the `tmc.overwrite_validations_file`-property which defaults to false. The output file will contain the `CheckstyleResult` serialised as JSON.

    java -Dtmc.project_dir=[PROJECT-DIRECTORY-PATH] -Dtmc.validations_file=[OUTPUT-FILE-PATH] -Dtmc.locale=[LOCALE] -Dtmc.overwrite_validations_file=[BOOLEAN] -jar tmc-checkstyle-runner-1.0.1.jar

## Configuration

Running Checkstyle validations is enabled when the strategy has been set to `FAIL` or `WARN`. You can configure the runner by creating a `.tmcproject.json`-configuration file to the root of the project to be tested. You may also use YAML for configuration files (`.tmcproject.yml`).

By default, a custom Checkstyle-configuration file `.checkstyle.xml` is searched from the project root. You can also specify a custom filename with the `rule`-option. A custom filename must end with `checkstyle.xml`. If neither is found, the default bundled configuration will be used.

**.tmcproject.json**

```json
{
    "checkstyle": {

        "rule": "mooc-checkstyle.xml",
        "strategy": "FAIL"

    }
}
```

**.tmcproject.yml**

```yaml
checkstyle:

    rule: mooc-checkstyle.xml
    strategy: WARN
```

### Options

* `rule` — the name of a custom [Checkstyle-configuration](http://checkstyle.sourceforge.net/config.html) file. Should be in the root of the project and must end with `checkstyle.xml`. See [default-checkstyle.xml](src/main/resources/default-checkstyle.xml) for the default configuration.
* `strategy` — the strategy that is used to run Checkstyle-validations (`FAIL`, `WARN` or `DISABLED`), `DISABLED` by default.

## Credits

This project has been developed at the University of Helsinki’s [Department of Computer Science](http://cs.helsinki.fi/en/) by:

* Kenny Heinonen ([kennyhei](https://github.com/kennyhei/))
* Kasper Hirvikoski ([kasper](https://github.com/kasper/))
* Jarmo Isotalo ([jamox](https://github.com/jamox/))
* Joni Salmi ([josalmi](https://github.com/josalmi/))

## License

This project is licensed under [GPL2](LICENSE.txt).
