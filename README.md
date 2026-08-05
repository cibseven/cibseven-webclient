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

#### Date and Time Serialization

Date and time values in REST responses are serialized as ISO-8601 strings, matching the Spring Boot default:

| Type | Output |
|------|--------|
| `java.util.Date`, `java.sql.Timestamp` | `"2026-08-04T13:20:00.000+00:00"` |
| `Instant` | `"2026-08-04T13:20:00Z"` |
| `OffsetDateTime` | `"2026-08-04T12:00:00+02:00"` |
| `LocalDateTime` | `"2026-08-04T12:00:00"` |
| `LocalDate` | `"2026-08-04"` |
| `Duration` | `"PT1H"` |

Up to and including 2.2.1, the webclient's own `ObjectMapper` overrode the Spring Boot defaults and wrote epoch
millis instead (`1785849600000`), and numeric arrays or fractional seconds for `java.time` types. Consumers built
against that output have to parse ISO-8601 strings now.

As a temporary migration aid, the previous output can be restored:

```yaml
cibseven:
  webclient:
    custom:
      spring:
        jackson:
          serialization:
            write-dates-as-timestamps: true   # deprecated, default: false
```

**This property is deprecated and will be removed.** It exists only to give consumers that adapted to the earlier
output a migration window. Jackson 3 disables timestamp output by default, so the flag loses its purpose once the
wire layer moves to Jackson 3 and will be removed together with that migration.

Prefer adapting the consumer, or annotating the individual field with `@JsonFormat`, over switching the whole
application back. Note that the flag is global: it affects every date and duration in every REST response.

### Documentation

- [CIB seven Manual](https://docs.cibseven.org/manual/latest/)
- [Migration Guide](https://github.com/cibseven/cibseven-migration)
- [Docker Setup](https://github.com/cibseven/cibseven-docker)

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
