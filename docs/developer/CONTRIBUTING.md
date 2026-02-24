# How to Contribute

## Requirements

- Java 21.
- Maven 3.9.9 or newer. You may need to add mirror sites to speedup download.
- Git 2.23 or newer.
- The latest Eclipse SDK from https://download.eclipse.org/eclipse/downloads/ .

## Steps

- Forking this repository on GitHub.
- Cloning your repository to your disk.
- Create a new branch from "main".
- Coding.
- Testing changes by using "Run As Eclipse Application" in Eclipse SDK.
- Building the update site by running `mvn clean verify` before creating PRs. You can check built files under the "sites/repository/target/repository/" directory.

## Notes

- Commit message should be composed properly by following [Conventional Commits](https://www.conventionalcommits.org/).
- Ensure every commit includes a "Signed-off-by" line to comply with the Developer Certificate of Origin (DCO). By signing off on your commits, you certify that this contribution complies with the [DCO 1.1](https://developercertificate.org/).
