
# Maple-MDK-26.1.2-ModDevGradle

本库基于 NeoForge MDK 修改，旨在简化模组开发流程，提供更好的开发体验。

## 修改内容

- **Gradle 配置优化**：整合版本管理，统一依赖配置
- **依赖更新**：适配 Minecraft 26.1.2 和 NeoForge 26.1.2.71+
- **配置系统增强**：提供完整的配置示例和注释
- **代码结构优化**：清晰的目录结构和代码组织

## 开发环境要求

- JDK 25 或更高版本
- IntelliJ IDEA 或 Eclipse
- Gradle 9.x

## 鸣谢

本项目参考了以下优秀开源项目：

- [GregTechCEu](https://github.com/GregTechCEu) - 模组开发框架参考
- [GregTech-Odyssey](https://github.com/GregTech-Odyssey) - 模组开发框架参考
- [Creators-of-Create](https://github.com/Creators-of-Create) - Gradle 配置参考

感谢这些项目的开发者们分享他们的知识和代码！

---

Installation information
=======

This template repository can be directly cloned to get you started with a new
mod. Simply create a new repository cloned from this one, by following the
instructions provided by [GitHub](https://docs.github.com/en/repositories/creating-and-managing-repositories/creating-a-repository-from-a-template).

Once you have your clone, simply open the repository in the IDE of your choice. The usual recommendation for an IDE is either IntelliJ IDEA or Eclipse.

If at any point you are missing libraries in your IDE, or you've run into problems you can
run `gradlew --refresh-dependencies` to refresh the local cache. `gradlew clean` to reset everything 
{this does not affect your code} and then start the process again.

Mapping Names:
============
By default, the MDK is configured to use the official mapping names from Mojang for methods and fields 
in the Minecraft codebase. These names are covered by a specific license. All modders should be aware of this
license. For the latest license text, refer to the mapping file itself, or the reference copy here:
https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

Additional Resources: 
==========
Community Documentation: https://docs.neoforged.net/  
NeoForged Discord: https://discord.neoforged.net/