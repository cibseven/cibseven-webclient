# CIB seven webclient

[![License](https://img.shields.io/github/license/cibseven/cibseven-webclient?color=blue&logo=apache)](https://github.com/cibseven/cibseven-webclient/blob/master/LICENSE)
![Build Status](https://img.shields.io/badge/build-internal-lightgrey)
[![Version](https://img.shields.io/github/v/release/cibseven/cibseven-webclient)](https://github.com/cibseven/cibseven-webclient/releases)

The **CIB seven webclient** is the official web application interface for the [CIB seven](https://github.com/cibseven) process automation platform. It includes the **Cockpit**, **Tasklist**, and **Admin** applications for process monitoring, user task management, and administrative controls.

## Overview

This project contains the following integrated top-level web applications:

- **Cockpit** – Monitoring and operation of running process instances.
- **Tasklist** – Interface for end-users to complete and manage workflow tasks.
- **Admin** – Management of users, groups, and authorizations.

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- Node.js (for frontend development tasks)

### Build

Clone the repository and build all applications using Maven:

```bash
git clone https://github.com/cibseven/cibseven-webclient.git
cd cibseven-webclient
mvn clean install
```

### Configuration

The webclient can be configured using Spring Boot properties. Key configuration options include:

#### Where settings and defaults are defined

All settings below `cibseven.webclient` and their defaults are defined in one class,
[`SevenWebclientProperties`](cibseven-interfaces/src/main/java/org/cibseven/webapp/config/SevenWebclientProperties.java).
The webclient no longer ships an `application.yaml`. Put your own settings in `cibseven-webclient.yaml`
or `cibseven-webclient.properties` on the classpath, or use any other Spring Boot property source
(environment variables, system properties, an external `application.yaml`).

The webclient's tables (modeler, folders, history) live in its own JPA persistence unit, so the global
`spring.jpa.*` settings of your application do not apply to them and vice versa. Hibernate settings for
that unit go under `cibseven.webclient.persistence.properties`, for example
`cibseven.webclient.persistence.properties.hibernate.default_schema`.

#### Defaults of the standalone application

When the webclient runs as its own application (`java -jar` or a WAR on Tomcat/WildFly), it additionally
sets the following framework defaults, see
[`StandaloneDefaults`](cibseven-webclient-core/src/main/java/org/cibseven/webapp/config/StandaloneDefaults.java).
They have the lowest precedence, so any configured value overrides them.

| Property | Default | Spring Boot's own default |
|---|---|---|
| `spring.config.import` | `optional:classpath:cibseven-webclient.properties,optional:classpath:cibseven-webclient.yaml` | – |
| `spring.servlet.register-default-servlet` | `true` | `false` |
| `spring.servlet.multipart.max-file-size` | `10MB` | `1MB` |
| `spring.servlet.multipart.max-request-size` | `1GB` | `10MB` |
| `springdoc.swagger-ui.path` | `/swagger-ui` | `/swagger-ui.html` |
| `springdoc.swagger-ui.operationsSorter`, `tagsSorter` | `alpha` | unsorted |
| `springdoc.swagger-ui.doc-expansion` | `none` | `list` |
| `management.*` | only `health` and `info` exposed, health probes enabled | – |

When the webclient is embedded into another Spring Boot application, none of these are set: your
application keeps full control over its `spring.*`, `springdoc.*` and `management.*` settings.

#### Engine REST API Configuration

```yaml
cibseven:
  webclient:
    engineRest:
      url: http://localhost:8080          # Base URL of the CIB seven engine
      path: /engine-rest                  # Configurable REST API path (default: /engine-rest)
```

The `path` property allows you to customize the engine REST API path to support different Jersey application configurations. For example, if your Spring Boot application defines a custom Jersey path:

```yaml
# Custom Jersey path example
cibseven:
  webclient:
    engineRest:
      url: http://localhost:8080
      path: /different-path               # Custom path instead of default /engine-rest
```

#### Development Configuration

When developing the frontend, if you are using a custom engine REST path, you should also update the frontend development proxy configuration in `frontend/vite.config.js`:

```javascript
// Set ENGINE_REST_PATH environment variable to match your backend configuration
const engineRestPath = process.env.ENGINE_REST_PATH || '/engine-rest'
```

You can set this environment variable when running the development server:

```bash
# For custom path
ENGINE_REST_PATH=/different-path npm run dev

# For default path (no environment variable needed)
npm run dev
```

### Documentation

- [CIB seven Manual](https://docs.cibseven.org/manual/latest/)
- [Migration Guide](https://github.com/cibseven/cibseven-migration)
- [Docker Setup](https://github.com/cibseven/cibseven-docker)
- [Webclient plugins](PLUGINS.md)

## Contributing

We welcome contributions!

Have a look at our [contribution guide](https://github.com/cibseven/cibseven/blob/master/CONTRIBUTING.md) for how to contribute to this repository.

1. Fork the repository.
2. Create your feature branch: `git checkout -b feature/my-feature`
3. Commit your changes: `git commit -am 'Add new feature'`
4. Push to the branch: `git push origin feature/my-feature`
5. Submit a pull request.

## Releases

Check out the [Releases](https://github.com/cibseven/cibseven-webclient/releases) page for version history, features, and patch notes.

## License

This project is licensed under the Apache 2.0 License – see the [LICENSE](LICENSE) file for details.

CIB seven uses and includes third-party dependencies published under various licenses. By downloading and using CIB seven artifacts, you agree to their terms and conditions. Refer to https://docs.cibseven.org/manual/latest/introduction/third-party-libraries/ for an overview of third-party libraries and particularly important third-party licenses we want to make you aware of.

## Authors

Developed and maintained by the [CIB software GmbH](https://www.cib.de).

## Related Repositories

- [CIB seven Core](https://github.com/cibseven/cibseven)
- [CIB seven Docker](https://github.com/cibseven/cibseven-docker)
- [CIB seven Migration](https://github.com/cibseven/cibseven-migration)
