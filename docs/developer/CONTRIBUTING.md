# How to Contribute

## Requirements

- Java 21.
- Maven 3.9.9 or newer, but not Maven 4. You may need to add mirror sites to speed up downloads.
- Git 2.23 or newer.
- The latest Eclipse SDK from https://download.eclipse.org/eclipse/downloads/ .
- Install "M2E - Complete Development Kit" from m2eclipse for Maven integration in Eclipse. See https://projects.eclipse.org/projects/technology.m2e .

## Steps

- Forking this repository on GitHub.
- Cloning your repository to your disk.
- Create a new branch from "main".
- Coding.
- Testing changes by using "Run As Eclipse Application" in Eclipse SDK.
- Building the update site by running `./mvnw clean verify` before creating PRs. You can check built files under the "sites/repository/target/repository/" directory.

## Notes

- Commit message should be composed properly by following [Conventional Commits](https://www.conventionalcommits.org/).
- Ensure every commit includes a "Signed-off-by" line to comply with the Developer Certificate of Origin (DCO). By signing off on your commits, you certify that this contribution complies with the [DCO 1.1](https://developercertificate.org/).

# How to Release a new version

Steps:

- Update hard-coded version number by running `./mvnw tycho-versions:set-version -DnewVersion=X.Y.Z-SNAPSHOT` and make a commit:
  - Use tag `vX.Y.Z-beta.N` for beta versions.
  - Use tag `vX.Y.Z` for final versions.
  - The tag `continuous` is used by the CI, so developers should NOT touch it.
  - Do NOT use any other string as the tag name.
- Send the tag to GitHub:
  - If you're able to push a tag to the "main" branch, just tag the current commit and push the tag.
  - If you have no privilege to do so, use GitHub's infrastructure instead. Make new tags on the "main" branch by creating and immediately deleting a new release in the "Release" page on GitHub Web UI. Don't delete the tag as well. You can also trigger the action in the "Actions" page and manually fire the CI with a valid version number on the "main" branch.

All version numbers are monotonic. Keep moving forward.
