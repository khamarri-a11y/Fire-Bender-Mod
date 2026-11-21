# Build your Fire Bender mod without installing Gradle

1. Sign in to https://github.com and create a new **public or private** repository.
2. Upload **the contents** of this ZIP to the repository root (including the hidden `.github` directory). GitHub's web upload may hide dot-folders on some systems; use GitHub Desktop if needed.
3. Open the repository **Actions** tab and enable workflows if prompted.
4. Choose **Build Falcon Soulfire Wings**, click **Run workflow**, then **Run workflow** again.
5. Wait for the run. If green, open it and download the artifact named `falcon-soulfire-wings-fabric-1.20.1`.
6. Extract that artifact ZIP. Copy the main mod `.jar` (not a `-sources.jar`) into your Modrinth Fabric 1.20.1 profile's `mods` folder.
7. Install Fabric API, Origins and the matching Fire Bender datapack separately.

**Important:** The Java source was not compiled or Minecraft-tested here. The online build can fail due to Java API incompatibilities, Fabric mappings, or other issues. If the build turns red, open the failed `Build Fabric mod` step and share the error output for debugging. A green build still does not guarantee correct in-game behavior.
