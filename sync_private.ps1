# SYNC_PRIVATE.PS1 - ISOLATED METHOD
#
# This script copies the entire working directory (including ignored files)
# to a temporary location, performs the commit/push there, and then cleans up.
# This ensures your primary local repository (.git directory) is never touched
# and cannot be corrupted.
#
# Usage: .\sync_private.ps1 "Your commit message here"

param(
    [Parameter(Mandatory=$true)]
    [string]$CommitMessage
)

# --- 1. CONFIGURATION & SETUP ---
# The URL has been set to your specified private repository.
$NewRepoUrl = "https://github.com/fru-art/fru-scripts-private.git"
$TargetBranchName = "main"

# Define the paths
$OriginalDir = (Get-Location).Path
$TempDirName = "temp_private_sync_" + [System.Guid]::NewGuid().ToString("N")
$TempDir = Join-Path $OriginalDir $TempDirName

Write-Host "Starting ISOLATED sync for private repository..."
Write-Host "Source Directory: $OriginalDir" -ForegroundColor Yellow

# --- 2. ISOLATION & COPY ---
Write-Host "Copying all project files to temporary directory: $TempDirName..."
try {
    # Create the temporary directory
    New-Item -Path $TempDir -ItemType Directory -Force | Out-Null

    # Copy all files and folders, including hidden files (.git, .gitignore, etc.)
    # We explicitly exclude the temp directory itself to prevent infinite copy loop
    Get-ChildItem $OriginalDir -Force | Where-Object { $_.Name -ne $TempDirName } | ForEach-Object {
        $SourcePath = $_.FullName
        $DestinationPath = Join-Path $TempDir $_.Name
        # Use Copy-Item -Recurse -Force for both files and directories
        Copy-Item $SourcePath -Destination $DestinationPath -Recurse -Force -ErrorAction Stop
    }

} catch {
    Write-Error "ERROR: Failed to copy files to temporary directory. $($_.Exception.Message)"
    Remove-Item $TempDir -Recurse -Force -ErrorAction SilentlyContinue
    exit 1
}

# --- 3. COMMIT AND PUSH FROM ISOLATED REPO ---
Write-Host "Creating isolated repository and committing all files..."
try {
    # Change into the temporary directory
    Set-Location $TempDir

    # Initialize a new Git repository (without forcing a branch name yet)
    git init -q

    # FIX: Explicitly switch to the target branch (or create it) to resolve the 'src refspec' error
    Write-Host "Switching to target branch '$TargetBranchName'..."
    git switch -C $TargetBranchName > $null 2>&1

    # Force add ALL files, including the copied .git directory and .gitignore
    Write-Host "Forcing addition of all files..."
    git add -A -f > $null 2>&1

    # Commit the snapshot
    Write-Host "Committing all files with message: '$CommitMessage'"
    $CommitOutput = git commit -m $CommitMessage 2>&1

    if ($CommitOutput -notlike "*nothing to commit*") {
        Write-Host "Commit successful." -ForegroundColor Green
    } else {
        Write-Error "Commit failed: Git reported 'nothing to commit' in temporary repo."
        throw "Commit Failed"
    }

    # Push
    Write-Host "Pushing commit to private remote: $NewRepoUrl..."
    # The -q flag is added here to suppress standard push output.
    git push $NewRepoUrl $TargetBranchName --force --set-upstream -q 2>&1
    Write-Host "Push successful to $NewRepoUrl" -ForegroundColor Green

} catch {
    $ErrorOutput = $_.Exception.Message
    Write-Error "A fatal error occurred during commit/push in the isolated repository. Error: $($ErrorOutput)"
    # Clean up will occur in the finally block
    throw "Fatal Isolation Failure"
} finally {
    # --- 4. CLEANUP AND REVERT ---
    Write-Host "Cleaning up local repository..."

    # Change back to the original directory
    Set-Location $OriginalDir

    # Recursively delete the entire temporary directory
    Write-Host "Deleting temporary directory: $TempDirName"
    Remove-Item $TempDir -Recurse -Force -ErrorAction SilentlyContinue

    Write-Host "--------------------------------------------------------" -ForegroundColor Cyan
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Sync Complete. All files committed and pushed to the private repository." -ForegroundColor Green
    } else {
        Write-Host "Sync Complete, but ended with errors. Check the private remote." -ForegroundColor Red
    }
    Write-Host "--------------------------------------------------------" -ForegroundColor Cyan
}