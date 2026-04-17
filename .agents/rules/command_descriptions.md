# Command Execution Rule (Hybrid Model)

1. **General Commands**: You are allowed to execute general terminal commands (e.g., `git status`, `ls`, `find`, `grep`, `cat`) to research the codebase and manage meta-tasks.
2. **Build Restrictions**: You must NEVER execute build commands (e.g., `./gradlew ...`, `npm run build`). Instead, you must propose the command to the user and wait for them to run it.
3. **Push Restrictions**: You must NEVER execute `git push` commands. Propose the push command to the user for them to execute.
4. **Assume Success**: Unless the user explicitly pastes an Error or Failure output after a manual build/push, you should assume the command was successful and proceed with the next step.
5. **Justification**: For every command you DO execute directly, you must still provide a brief description beforehand explaining what it does and why.
