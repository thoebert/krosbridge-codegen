# krosbridge-codegen

A code generator for the required message/service data classes for [krosbridge](https://github.com/thoebert/krosbridge-codegen). 

## Functionality

[krosbridge](https://github.com/thoebert/krosbridge-codegen) relies on kotlin data classes for the serialization of custom message/service types. These serialization classes can be either written or generated using this software.
In summary, the code generator reads the user-defined `.msg`/`.srv` files with the different type definitions to generate the required kotlin data classes.

This package is a standalone command line java application and a Gradle Plugin which exposes the gradle task `generateROSSources`.
* The standalone command line application requires the input and output paths for code generation.
* The gradle plugin searches for ROS types in the `src/main/ros/myrosproject/srv` directory to generate the sources into the build directory under `/build/generated/source/ros/com/company/project/messages/`.

## Usage

See [krosbridge](https://github.com/thoebert/krosbridge-codegen) for usage.

## Contributing

Feel free to open a new issue/pull-request about any possible improvement.

## Author

* [Timon Höbert](https://github.com/thoebert)

## License

This project is licensed under the BSD - see the [License.md](License.md) file for details.
